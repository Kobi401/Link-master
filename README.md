
# **Link Browser**

**Current Version**: `Prototype Build v0.1`  
**Build Date**: `2023-09-30`

Link Browser is a lightweight, performance-focused web browser written in **Java** (`OpenJDK 23`) using the **JavaFX** framework. It is designed to provide a simple yet powerful browsing experience without relying on Google-backed or other major data-harvesting search engines. Our primary goal is to maintain a browser that's not just built for performance but also for **user privacy** and **customization**.

I keep forgetting i need to add Comments and Documentation but some methods have comments and docs to them if i remembered too do it -Kobi401

## **Features**
- **Custom User-Agent String**: Built to emulate various user-agent strings for bypassing blocks, but defaults to a custom `LinkEngine` signature.
- **Tabbed Browsing**: Supports multi-tabbed browsing with an advanced tab manager, including memory monitoring for each tab.
- **Integrated Flash Support**: Flash support through `Ruffle` for legacy SWF files and Flash-based games.
- **Custom Settings Management**: A configuration manager that saves and loads user settings, with support for enabling/disabling Flash and other advanced options.
- **Customizable Status Bar**: Styled like the classic Internet Explorer status bar, with a progress indicator.
- **Internal Page Support**: Built-in pages like `About`, `Settings`, and future support for `History` and `Bookmarks`.
- **Developer-Friendly Menu**: Developer-focused tools like page refresh, forward, back, and a menu system that can be extended.
- **Privacy-Oriented Browsing**: Designed to avoid using big-company search engines by default, with options to use privacy-focused search providers.
- **Loading Bar & Page Monitoring**: Tracks and displays the progress of loading a page with indicators.

## **Upcoming Features**
- **Enhanced Java Version Compatibility**: Support planned for older Java versions (`Java 8 - 11`) to bring compatibility to legacy systems like Windows XP and Windows 7.
- **Offline HTML Rendering Engine**: A fully custom HTML/CSS rendering engine to reduce dependency on JavaFX's built-in WebView.
- **Built-In Web Development Tools**: Tools similar to Chrome DevTools for inspecting HTML, CSS, and JavaScript.
- **Browser Extensions**: A custom extension system for adding new features without recompiling.
- **User Profiles & Sync**: Allow multiple users to use the same browser installation with separate profiles.

## **Planned Windows XP & 8.1 Compatibility** *(Requested by Kobi401)*
Link Browser is being developed with legacy support in mind. Although our primary build target is for modern systems (Windows 10 and above), we have planned support for older Java versions, enabling the browser to work seamlessly on Windows XP, Vista, and Windows 8.1. Note that these versions will require a backport of some features and are **not currently supported**. Keep an eye on future releases as we expand our compatibility matrix.

## **Contributing**
We welcome contributions! If you’re interested in developing new features, fixing bugs, or enhancing performance, please follow the steps below:

1. **Fork** the repository.
2. Create a new **branch** for your feature or bugfix (`git checkout -b feature-name`).
3. **Commit** your changes (`git commit -m 'Add new feature'`).
4. **Push** the branch (`git push origin feature-name`).
5. Submit a **pull request** for review.

## **Known Issues**
- **Flash Compatibility**: Some Flash-based content may not render correctly due to Ruffle limitations.
- **Memory Optimization**: Multi-tabbed browsing on low-memory systems can result in slow performance.
  
## **Developer Notes**
- Link Browser is being actively developed and maintained by **Kobi401**, the primary developer for the core API, backend functionality, and overall architecture.
- UI design and security features are being planned and implemented collaboratively by **Im_Owen25**, contributing to frontend design and browser security layers. *(Contribution pending)*.

## **License**
This project is licensed under the MIT License. See the [LICENSE](./LICENSE) file for more details.

## **Libraries Used**
Link Browser relies on the following libraries and frameworks:

- **JavaFX**: For UI rendering and WebView integration.
- **Ruffle**: Integrated Flash emulator for legacy Flash content.

These libraries are licensed under their respective licenses, such as MIT and Apache 2.0. Please refer to their documentation for more details.
