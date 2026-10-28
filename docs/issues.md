# Issue status

| ID | Issue | Priority | Status and evidence |
| --- | --- | --- | --- |
| 01 | Redundant **Choose an image** buttons | High | Resolved in code: removed the hero button; the workspace has one image picker alongside Reset. |
| 02 | Oversized preview and excessive scrolling | High | Resolved in code: preview is capped at 230 dp, uses FIT_CENTER, and the analysis workspace now appears near the top. Small-screen visual verification is still pending. |
| 03 | Redundant About content and navigation | High | Resolved in code: removed the top-right About navigation and redundant closing section; the workspace retains its task-specific instructions. |
| 04 | MOCK/DEMO requires an image | Medium | Resolved in code: mock mode enables processing without an image; success, empty, and simulated-error paths are isolated from the live API callback. Automated tests were added but could not run because Java is unavailable in this environment. |
| 05 | Dark scenario selector text is hard to read | Medium | Resolved in code: selected and dropdown rows use light text on dark surfaces. Device visual verification is pending. |
| 06 | Long description and workflow occupy space | Low | Resolved in code: removed the feature cards, workflow steps, and closing copy; the primary image workspace now appears at the top of the page. |
| 07 | Prepare for a physical Android phone | Medium | Partially resolved: manifest INTERNET permission, application ID, minSdk 24, targetSdk 36, system document image picker, and HTTPS default are present. Build/install and real-device checks remain pending because Java/Android tooling and a device were unavailable. No cleartext exception or API secret was added. |

