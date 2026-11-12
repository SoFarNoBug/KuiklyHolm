import SwiftUI
import holmDemo

struct ContentView: View {

    var body: some View {
        KuiklyRenderViewPage(pageName: "home", data: [:]).ignoresSafeArea()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}