import SwiftUI
import WatchKit

struct ContentView: View {

    @StateObject private var model = TimerModel()

    // Tracks raw crown rotation; delta is fed to the model.
    @State private var crownValue: Double = 0
    @State private var prevCrown: Double = 0

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 6) {
                // Session label
                Text(model.current.label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(model.current.isFocus ? .orange : .green)
                    .opacity(0.85)

                // Remaining time — the main display
                Text(model.timeString)
                    .font(.system(size: 54, weight: .bold, design: .monospaced))
                    .foregroundColor(.white)
                    .minimumScaleFactor(0.7)

                // Running / paused hint
                Text(model.isRunning ? "running" : "paused")
                    .font(.system(size: 11))
                    .foregroundColor(.gray)
                    .opacity(0.55)
            }
        }
        // Crown focus + rotation — only meaningfully adjusts time when paused
        .focusable()
        .digitalCrownRotation(
            $crownValue,
            sensitivity: .low,
            isContinuous: true,
            isHapticFeedbackEnabled: false   // manual haptics in model
        )
        .onChange(of: crownValue) { newValue in
            let delta = newValue - prevCrown
            prevCrown = newValue
            model.adjustTime(crownDelta: delta)
        }
        .onAppear {
            crownValue = 0
            prevCrown = 0
        }
        // Full-screen tap = start / pause
        // On Apple Watch Ultra, wire this same call to the Action button via
        // an App Intent or WKExtendedRuntimeSession delegate.
        .onTapGesture {
            model.toggleTimer()
        }
    }
}

#Preview {
    ContentView()
}
