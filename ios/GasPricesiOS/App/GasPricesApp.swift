import SwiftUI

@main
struct GasPricesApp: App {
    var body: some Scene {
        WindowGroup {
            GasPricesView()
                .preferredColorScheme(.dark)
        }
    }
}