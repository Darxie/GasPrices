import Foundation

protocol GasPriceRepositoryProtocol {
    func fetchGasPrices() async throws -> GasPriceData
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
    static let baseURL = "https://statdata.statistics.sk/public/api/dc"
    static let downloadURL = "\(baseURL)/opendata/cube/dataset/sp0207ts/download?type=CSV&lang=EN"
    static let infoURL = "\(baseURL)/public/sp0207ts/info"
    static let sourceURL = "https://statdata.statistics.sk/public/ui/dc/"
    static let defaultLabel = "Average fuel prices in the Slovak Republic (weekly)"

    private let session: URLSession
    private let weeksToShow: Int

    init(session: URLSession = .shared, weeksToShow: Int = 30) {
        self.session = session
        self.weeksToShow = weeksToShow
    }

    func fetchGasPrices() async throws -> GasPriceData {
        let archive = try await get(Self.downloadURL)

        let rows: [FuelPriceCSVParser.Row]
        do {
            rows = try FuelPriceCSVParser.parseZippedRows(archive)
        } catch {
            throw GasPriceRepositoryError.decodingFailed(error.localizedDescription)
        }
        let weeks = Array(FuelPriceCSVParser.weeklyPrices(from: rows).suffix(weeksToShow))

        // Metadata is optional: prices are still shown if the info endpoint fails.
        let info: DatasetInfo? = try? await JSONDecoder().decode(DatasetInfo.self, from: get(Self.infoURL))

        return GasPriceData(
            label: info?.nameEn ?? Self.defaultLabel,
            updated: Self.formatUpdated(info?.lastDataChange) ?? weeks.last?.dateTo ?? "",
            sourceURL: Self.sourceURL,
            weeks: weeks
        )
    }

    private func get(_ urlString: String) async throws -> Data {
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

        return data
    }

    /// "2026-11-06T09:00:00+01:00" -> "06.11.2026 09:00" (in the timestamp's own offset).
    static func formatUpdated(_ isoDateTime: String?) -> String? {
        guard let isoDateTime else {
            return nil
        }
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = parser.date(from: isoDateTime) ?? {
            parser.formatOptions = [.withInternetDateTime]
            return parser.date(from: isoDateTime)
        }()
        guard let date else {
            return isoDateTime
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "dd.MM.yyyy HH:mm"
        formatter.timeZone = timeZone(ofISOString: isoDateTime) ?? .current
        return formatter.string(from: date)
    }

    private static func timeZone(ofISOString value: String) -> TimeZone? {
        if value.hasSuffix("Z") {
            return TimeZone(secondsFromGMT: 0)
        }
        guard value.count >= 6 else {
            return nil
        }
        let suffix = value.suffix(6) // "+01:00"
        let sign: Int
        switch suffix.first {
        case "+": sign = 1
        case "-": sign = -1
        default: return nil
        }
        let parts = suffix.dropFirst().split(separator: ":")
        guard parts.count == 2, let hours = Int(parts[0]), let minutes = Int(parts[1]) else {
            return nil
        }
        return TimeZone(secondsFromGMT: sign * (hours * 3600 + minutes * 60))
    }
}
