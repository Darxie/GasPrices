import Foundation

@MainActor
final class GasPricesViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var statusText = AppStrings.appName
    @Published var latestResponse: GasPricesResponse?
    @Published var latestPrices = FuelPriceTriple(gasoline95: nil, gasoline98: nil, diesel: nil)
    @Published var showRegressionLines = true

    private let repository: GasPriceRepositoryProtocol
    private var hasLoadedOnce = false
    private var loadTask: Task<Void, Never>?

    init(repository: GasPriceRepositoryProtocol = GasPriceRepository()) {
        self.repository = repository
    }

    deinit {
        loadTask?.cancel()
    }

    func loadIfNeeded() {
        guard !hasLoadedOnce else {
            return
        }

        hasLoadedOnce = true
        refreshPrices()
    }

    func refreshPrices() {
        statusText = AppStrings.loadingStatus
        loadGasPrices()
    }

    func toggleRegressionLines() {
        showRegressionLines.toggle()
    }

    func sharePayload() -> String? {
        guard let response = latestResponse else {
            statusText = AppStrings.dataNotReady
            return nil
        }

        return GasPriceTransformer.buildShareText(response: response)
    }

    func formattedHeaderPrice(_ value: Double?) -> String {
        GasPriceTransformer.formatPrice(value, addCurrency: true)
    }

    func loadGasPrices() {
        loadTask?.cancel()
        loadTask = Task { [weak self] in
            guard let self else {
                return
            }

            self.isLoading = true
            defer { self.isLoading = false }

            do {
                let response = try await self.repository.fetchGasPrices()
                guard !Task.isCancelled else {
                    return
                }

                self.latestResponse = response
                self.errorMessage = nil
                self.statusText = "\(response.label)\n\(AppStrings.updatedLabel(response.update))"
                self.latestPrices = GasPriceTransformer.latestAvailableFuelPrices(from: response.value)
            } catch {
                guard !Task.isCancelled else {
                    return
                }

                self.latestResponse = nil
                self.errorMessage = error.localizedDescription.isEmpty ? "Failed to load gas prices." : error.localizedDescription
                self.statusText = self.errorMessage ?? "Failed to load gas prices."
                self.latestPrices = FuelPriceTriple(gasoline95: nil, gasoline98: nil, diesel: nil)
            }
        }
    }
}