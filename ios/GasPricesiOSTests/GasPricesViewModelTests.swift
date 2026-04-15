import Foundation
import XCTest
@testable import GasPricesiOS

@MainActor
final class GasPricesViewModelTests: XCTestCase {
    func testLoadGasPricesSuccessUpdatesState() async {
        let repository = StubGasPriceRepository(result: .success(TestFixtures.sampleResponse()))
        let viewModel = GasPricesViewModel(repository: repository)

        viewModel.loadGasPrices()
        await waitUntilFinished(viewModel)

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNotNil(viewModel.latestResponse)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertEqual(viewModel.latestPrices.gasoline95, 1.500)
    }

    func testLoadGasPricesFailureSetsErrorAndClearsData() async {
        let repository = StubGasPriceRepository(result: .failure(StubError.network))
        let viewModel = GasPricesViewModel(repository: repository)

        viewModel.loadGasPrices()
        await waitUntilFinished(viewModel)

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.latestResponse)
        XCTAssertEqual(viewModel.errorMessage, "Network error")
    }

    private func waitUntilFinished(_ viewModel: GasPricesViewModel) async {
        for _ in 0..<100 {
            if !viewModel.isLoading && (viewModel.latestResponse != nil || viewModel.errorMessage != nil) {
                return
            }

            try? await Task.sleep(nanoseconds: 10_000_000)
        }
    }
}

private final class StubGasPriceRepository: GasPriceRepositoryProtocol {
    let result: Result<GasPricesResponse, Error>

    init(result: Result<GasPricesResponse, Error>) {
        self.result = result
    }

    func fetchGasPrices() async throws -> GasPricesResponse {
        try result.get()
    }
}

private enum StubError: LocalizedError {
    case network

    var errorDescription: String? {
        switch self {
        case .network:
            return "Network error"
        }
    }
}