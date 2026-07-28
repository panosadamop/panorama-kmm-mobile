import XCTest

final class ScreenshotUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func tap(_ app: XCUIApplication, devicePxX: CGFloat, devicePxY: CGFloat, deviceW: CGFloat, deviceH: CGFloat) {
        let window = app.windows.firstMatch
        let coordinate = window.coordinate(withNormalizedOffset: CGVector(dx: devicePxX / deviceW, dy: devicePxY / deviceH))
        coordinate.tap()
    }

    // MARK: - iPad 13" (2064x2752)
    // Each test launches fresh, navigates to exactly one target screen, then holds for a
    // long time before ending. The actual screenshot is captured externally via
    // `xcrun simctl io screenshot` (synced against a screen recording), because XCUITest's
    // own screenshot() API returns 2048x2732 here, not the exact 2064x2752 Apple requires.

    private let W: CGFloat = 2064
    private let H: CGFloat = 2752
    private let menuX: CGFloat = 55
    private let menuY: CGFloat = 115

    private func openDrawer(_ app: XCUIApplication) {
        tap(app, devicePxX: menuX, devicePxY: menuY, deviceW: W, deviceH: H)
        sleep(3)
    }

    func testIPadArticleList() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5)
        openDrawer(app)
        tap(app, devicePxX: 242, devicePxY: 1270, deviceW: W, deviceH: H) // "Πολιχνίτος" category
        sleep(10) // HOLD: article list
    }

    private func attach(_ screenshot: XCUIScreenshot, named name: String) {
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testIPadContact() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5)
        openDrawer(app)
        attach(app.screenshot(), named: "verify-01-drawer")
        tap(app, devicePxX: 242, devicePxY: 940, deviceW: W, deviceH: H) // "Επικοινωνία"
        sleep(2)
        attach(app.screenshot(), named: "verify-02-after-tap")
        sleep(8) // HOLD: contact
        attach(app.screenshot(), named: "verify-03-final")
    }

    func testIPadWhoWeAre() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5)
        openDrawer(app)
        tap(app, devicePxX: 242, devicePxY: 821, deviceW: W, deviceH: H) // "Ποιοι Είμαστε"
        sleep(10) // HOLD: who we are
    }

    func testIPadArticleDetail() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5)
        tap(app, devicePxX: 1030, devicePxY: 1665, deviceW: W, deviceH: H) // "ΣΗΜΑΝΤΙΚΑ" card
        sleep(10) // HOLD: article detail
    }

    func testIPadSearch() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5)
        tap(app, devicePxX: 1915, devicePxY: 115, deviceW: W, deviceH: H) // search icon
        sleep(10) // HOLD: search
    }
}
