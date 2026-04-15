import Foundation

protocol GasPriceRepositoryProtocol {
    func fetchGasPrices() async throws -> GasPricesResponse
}

enum GasPriceRepositoryError: LocalizedError {
    case invalidURL
    case invalidResponse
    case requestFailed(String)
    case decodingFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL for gas prices endpoint."
        case .invalidResponse:
            return "Invalid server response."
        case .requestFailed(let message):
            return message
        case .decodingFailed(let message):
            return "Failed to decode gas prices response: \(message)"
        }
    }
}

final class GasPriceRepository: GasPriceRepositoryProtocol {
    private let session: URLSession
    private let weekRangeGenerator: WeekRangeGenerator

    init(session: URLSession = .shared, weekRangeGenerator: WeekRangeGenerator = WeekRangeGenerator()) {
        self.session = session
        self.weekRangeGenerator = weekRangeGenerator
    }

    func fetchGasPrices() async throws -> GasPricesResponse {
        let weeks = weekRangeGenerator.getLast30Weeks()
        let urlString = "https://data.statistics.sk/api/v2/dataset/sp0207ts/\(weeks)/UKAZ01,UKAZ02,UKAZ04?lang=en"

        guard let url = URL(string: urlString) else {
            throw GasPriceRepositoryError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 30

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw GasPriceRepositoryError.requestFailed(error.localizedDescription)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw GasPriceRepositoryError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            let body = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw GasPriceRepositoryError.requestFailed("Error fetching gas prices: \(body)")
        }

        do {
            return try JSONDecoder().decode(GasPricesResponse.self, from: data)
        } catch {
            throw GasPriceRepositoryError.decodingFailed(error.localizedDescription)
        }
    }
}