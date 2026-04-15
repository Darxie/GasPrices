import Foundation
import XCTest
@testable import GasPricesiOS

final class GasPriceRepositoryTests: XCTestCase {
    override func tearDown() {
        super.tearDown()
        MockURLProtocol.requestHandler = nil
    }

    func testFetchGasPricesSuccessReturnsDecodedResponse() async throws {
        let session = makeSession()
        let expected = TestFixtures.sampleResponse()

        MockURLProtocol.requestHandler = { request in
            XCTAssertTrue(request.url?.absoluteString.contains("/api/v2/dataset/sp0207ts/") == true)
            XCTAssertTrue(request.url?.absoluteString.contains("/UKAZ01,UKAZ02,UKAZ04?lang=en") == true)

            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (response, TestFixtures.sampleJSON())
        }

        let repository = GasPriceRepository(
            session: session,
            weekRangeGenerator: WeekRangeGenerator(
                calendar: makeCalendar(),
                nowProvider: { Date(timeIntervalSince1970: 1_744_720_000) }
            )
        )

        let response = try await repository.fetchGasPrices()

        XCTAssertEqual(response.label, expected.label)
        XCTAssertEqual(response.value.count, expected.value.count)
        XCTAssertEqual(response.dimension.sp0207ts_tyz.category.label["w0"], expected.dimension.sp0207ts_tyz.category.label["w0"])
    }

    func testFetchGasPricesServerErrorThrowsBody() async {
        let session = makeSession()

        MockURLProtocol.requestHandler = { request in
            let response = HTTPURLResponse(url: request.url!, statusCode: 500, httpVersion: nil, headerFields: nil)!
            let data = Data("{\"message\":\"Server Error\"}".utf8)
            return (response, data)
        }

        let repository = GasPriceRepository(session: session)

        do {
            _ = try await repository.fetchGasPrices()
            XCTFail("Expected fetchGasPrices to throw")
        } catch {
            XCTAssertTrue(error.localizedDescription.contains("Server Error"))
        }
    }

    private func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [MockURLProtocol.self]
        return URLSession(configuration: configuration)
    }

    private func makeCalendar() -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: "sk_SK")
        calendar.firstWeekday = 2
        return calendar
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