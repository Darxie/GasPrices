import Foundation

enum AppStrings {
    static let appName = "Gas prices"
    static let octane95Label = "95 Octane"
    static let octane98Label = "98 Octane"
    static let dieselLabel = "Diesel"
    static let sourceNotice = "Data source: http://datacube.statistics.sk/"
    static let refreshPrices = "Refresh prices"
    static let toggleTrendLines = "Toggle trend lines"
    static let sharePrices = "Share latest prices"
    static let loadingStatus = "Loading latest prices..."
    static let dataNotReady = "Data has not been loaded yet."
    static let noDataAvailable = "No historical data available."
    static let notAvailableLabel = "N/A"
    static let priceLabel = "Price"

    static func updatedLabel(_ value: String) -> String {
        "Updated: \(value)"
    }

    static func sourceLabel(_ value: String) -> String {
        "Source: \(value)"
    }
}