import WatchKit
import Combine

struct PomodoroSession {
    let label: String
    let duration: Int // seconds
    let isFocus: Bool
}

@MainActor
final class TimerModel: ObservableObject {

    static let sessions: [PomodoroSession] = [
        PomodoroSession(label: "Focus 1",   duration: 25 * 60, isFocus: true),
        PomodoroSession(label: "Break",     duration:  5 * 60, isFocus: false),
        PomodoroSession(label: "Focus 2",   duration: 25 * 60, isFocus: true),
        PomodoroSession(label: "Break",     duration:  5 * 60, isFocus: false),
        PomodoroSession(label: "Focus 3",   duration: 25 * 60, isFocus: true),
        PomodoroSession(label: "Break",     duration:  5 * 60, isFocus: false),
        PomodoroSession(label: "Focus 4",   duration: 25 * 60, isFocus: true),
        PomodoroSession(label: "Long Break",duration: 15 * 60, isFocus: false),
    ]

    @Published var sessionIndex: Int = 0
    @Published var timeRemaining: Double = Double(sessions[0].duration)
    @Published var isRunning: Bool = false

    private var timerCancellable: AnyCancellable?

    var current: PomodoroSession { Self.sessions[sessionIndex] }
    private var count: Int { Self.sessions.count }

    // MARK: - Timer control (bound to screen tap / Action button)

    func toggleTimer() {
        isRunning ? pause() : start()
    }

    private func start() {
        isRunning = true
        timerCancellable = Timer
            .publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in self?.tick() }
        WKInterfaceDevice.current().play(.start)
    }

    private func pause() {
        isRunning = false
        timerCancellable = nil
        WKInterfaceDevice.current().play(.stop)
    }

    private func tick() {
        guard timeRemaining > 0 else {
            endSession()
            return
        }
        timeRemaining -= 1
    }

    private func endSession() {
        timerCancellable = nil
        isRunning = false
        sessionIndex = (sessionIndex + 1) % count
        timeRemaining = Double(current.duration)
        WKInterfaceDevice.current().play(.success)
    }

    // MARK: - Crown adjustment (only when paused)
    // delta: raw crown units from .digitalCrownRotation with .low sensitivity
    // Each unit ≈ 15 seconds; requires intentional rotation to move a full minute.

    func adjustTime(crownDelta delta: Double) {
        guard !isRunning, abs(delta) > 0.001 else { return }

        let secondsChange = delta * 15.0
        let newTime = timeRemaining + secondsChange

        if newTime >= Double(current.duration) {
            // Scroll past the top → advance to next session
            let next = (sessionIndex + 1) % count
            sessionIndex = next
            timeRemaining = Double(current.duration)
            WKInterfaceDevice.current().play(.directionUp)
        } else if newTime < 0 {
            // Scroll past zero → retreat to previous session, park at 0
            sessionIndex = (sessionIndex - 1 + count) % count
            timeRemaining = 0
            WKInterfaceDevice.current().play(.directionDown)
        } else {
            timeRemaining = newTime
        }
    }

    // MARK: - Formatting

    var timeString: String {
        let total = max(0, Int(timeRemaining))
        return String(format: "%02d:%02d", total / 60, total % 60)
    }
}
