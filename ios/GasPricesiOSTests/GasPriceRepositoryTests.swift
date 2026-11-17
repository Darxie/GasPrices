import Foundation
import XCTest
@testable import GasPricesiOS

final class GasPriceRepositoryTests: XCTestCase {
    override func tearDown() {
        super.tearDown()
        MockURLProtocol.requestHandler = nil
    }

    func testFetchGasPricesSuccessParsesZipAndKeepsLatestWeeksInOrder() async throws {
        let archive = TestFixtures.makeZip([
            ("structure.csv", "a,b"),
            ("codelists/e_week.csv", "code,name"),
            ("sp0207ts_CSV_EN.csv", TestFixtures.sampleCSV())
        ])
        let info = Data(#"{"nameEn":"Average fuel prices","lastDataChange":"2026-11-06T09:00:00+01:00"}"#.utf8)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!.absoluteString
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            if url == GasPriceRepository.downloadURL {
                return (response, archive)
            }
            XCTAssertEqual(url, GasPriceRepository.infoURL)
            return (response, info)
        }

        let repository = GasPriceRepository(session: makeSession(), weeksToShow: 2)
        let data = try await repository.fetchGasPrices()

        XCTAssertEqual(data.label, "Average fuel prices")
        XCTAssertEqual(data.updated, "06.11.2026 09:00")
        XCTAssertEqual(data.weeks.map(\.week), ["W43", "W44"])
        XCTAssertEqual(data.weeks[0].gasoline95, 1.820)
        XCTAssertNil(data.weeks[0].gasoline98)
        XCTAssertEqual(data.weeks[0].diesel, 1.970)
        XCTAssertEqual(data.weeks[1], WeeklyFuelPrices(
            year: "2026", week: "W44", dateFrom: "2026-10-26", dateTo: "2026-11-01",
            gasoline95: 1.785, gasoline98: 1.988, diesel: 1.979
        ))
    }

    func testFetchGasPricesInfoFailureStillReturnsPrices() async throws {
        let csv = TestFixtures.csvHeader + "\n"
            + TestFixtures.csvRow("CP_07202102", week: "W44", from: "2026-10-26", to: "2026-11-01", value: "1.785")
        let archive = TestFixtures.makeZip([("sp0207ts_CSV_EN.csv", csv)])

        MockURLProtocol.requestHandler = { request in
            let isDownload = request.url!.absoluteString == GasPriceRepository.downloadURL
            let response = HTTPURLResponse(url: request.url!, statusCode: isDownload ? 200 : 500, httpVersion: nil, headerFields: nil)!
            return (response, isDownload ? archive : Data())
        }

        let data = try await GasPriceRepository(session: makeSession()).fetchGasPrices()

        XCTAssertEqual(data.label, GasPriceRepository.defaultLabel)
        XCTAssertEqual(data.updated, "2026-11-01")
        XCTAssertEqual(data.weeks.count, 1)
    }

    func testFetchGasPricesServerErrorThrowsBody() async {
        MockURLProtocol.requestHandler = { request in
            let response = HTTPURLResponse(url: request.url!, statusCode: 500, httpVersion: nil, headerFields: nil)!
            return (response, Data("{\"message\":\"Server Error\"}".utf8))
        }

        do {
            _ = try await GasPriceRepository(session: makeSession()).fetchGasPrices()
            XCTFail("Expected fetchGasPrices to throw")
        } catch {
            XCTAssertTrue(error.localizedDescription.contains("Server Error"))
        }
    }

    func testFetchGasPricesZipWithoutDataCSVThrows() async {
        let archive = TestFixtures.makeZip([("structure.csv", "a,b")])
        MockURLProtocol.requestHandler = { request in
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (response, archive)
        }

        do {
            _ = try await GasPriceRepository(session: makeSession()).fetchGasPrices()
            XCTFail("Expected fetchGasPrices to throw")
        } catch {
            XCTAssertTrue(error.localizedDescription.contains("No data CSV"))
        }
    }

    func testParseCSVMissingColumnThrows() {
        XCTAssertThrowsError(try FuelPriceCSVParser.parseCSV(Data("e_coicop_code,value\nCP_07202102,1.5".utf8))) { error in
            XCTAssertEqual(error as? FuelPriceCSVParserError, .missingColumn("e_year_code"))
        }
    }

    func testWeeklyPricesIgnoresUnparseableValues() {
        let weeks = FuelPriceCSVParser.weeklyPrices(from: [
            .init(fuelCode: "CP_07202102", year: "2026", week: "W44", dateFrom: "2026-10-26", dateTo: "2026-11-01", value: ":")
        ])
        XCTAssertEqual(weeks.count, 1)
        XCTAssertNil(weeks[0].gasoline95)
    }

    func testZipReaderRejectsGarbage() {
        XCTAssertThrowsError(try ZipArchiveReader(data: Data("not a zip".utf8)))
    }

    private func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [MockURLProtocol.self]
        return URLSession(configuration: configuration)
    }
}

private final class MockURLProtocol: URLProtocol {
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let requestHandler = Self.requestHandler else {
            client?.urlProtocol(self, didFailWithError: URLError(.badServerResponse))
            return
        }

        do {
            let (response, data) = try requestHandler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {
    }
}
