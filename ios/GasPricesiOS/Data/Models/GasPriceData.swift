import Foundation

/// Average fuel prices for one week of dataset sp0207ts.
/// A price is nil when the statistics office did not publish it for that week.
struct WeeklyFuelPrices: Equatable {
    let year: String
    let week: String
    /// ISO date, e.g. "2026-10-26".
    let dateFrom: String
    /// ISO date, e.g. "2026-11-01".
    let dateTo: String
    let gasoline95: Double?
    let gasoline98: Double?
    let diesel: Double?
}

/// Weekly prices in chronological order (oldest first) plus dataset metadata.
struct GasPriceData: Equatable {
    let label: String
    let updated: String
    let sourceURL: String
    let weeks: [WeeklyFuelPrices]
}

/// Response of /public/api/dc/public/{dataset}/info.
struct DatasetInfo: Decodable {
    let nameEn: String?
    let lastDataChange: String?
}
