import SwiftUI

@MainActor
struct GasPricesView: View {
    @StateObject private var viewModel: GasPricesViewModel
    @State private var sharedText = ""
    @State private var isShareSheetPresented = false

    init() {
        _viewModel = StateObject(wrappedValue: GasPricesViewModel())
    }

    init(viewModel: GasPricesViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text(viewModel.statusText)
                    .font(.title3.bold())
                    .frame(maxWidth: .infinity, alignment: .leading)

                HStack(spacing: 16) {
                    PriceTile(
                        label: AppStrings.octane95Label,
                        value: viewModel.formattedHeaderPrice(viewModel.latestPrices.gasoline95),
                        color: Color(red: 1.0, green: 0.7569, blue: 0.0275)
                    )

                    PriceTile(
                        label: AppStrings.octane98Label,
                        value: viewModel.formattedHeaderPrice(viewModel.latestPrices.gasoline98),
                        color: Color(red: 0.2980, green: 0.6863, blue: 0.3137)
                    )

                    PriceTile(
                        label: AppStrings.dieselLabel,
                        value: viewModel.formattedHeaderPrice(viewModel.latestPrices.diesel),
                        color: Color(red: 0.1294, green: 0.5882, blue: 0.9529)
                    )
                }

                Group {
                    if let response = viewModel.latestResponse {
                        GasPriceLineChartView(
                            response: response,
                            showRegressionLines: viewModel.showRegressionLines
                        )
                    } else {
                        Text(viewModel.isLoading ? "" : AppStrings.noDataAvailable)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, alignment: .center)
                }

                Text(AppStrings.sourceNotice)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .opacity(0.6)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            .padding(20)
            .navigationTitle(AppStrings.appName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        viewModel.refreshPrices()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .accessibilityLabel(AppStrings.refreshPrices)
                }

                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            viewModel.toggleRegressionLines()
                        } label: {
                            if viewModel.showRegressionLines {
                                Label(AppStrings.toggleTrendLines, systemImage: "checkmark")
                            } else {
                                Text(AppStrings.toggleTrendLines)
                            }
                        }

                        Button(AppStrings.sharePrices) {
                            shareLatestPrices()
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .task {
            viewModel.loadIfNeeded()
        }
        .sheet(isPresented: $isShareSheetPresented) {
            ShareSheet(activityItems: [sharedText])
        }
        .preferredColorScheme(.dark)
    }

    private func shareLatestPrices() {
        guard let payload = viewModel.sharePayload() else {
            return
        }

        sharedText = payload
        isShareSheetPresented = true
    }
}

private struct PriceTile: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(value)
                .font(.headline.weight(.bold))
                .foregroundStyle(color)
                .minimumScaleFactor(0.75)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }
}