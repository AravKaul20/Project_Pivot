# Security Policy

## Supported Versions

We release patches for security vulnerabilities. Which versions are eligible for receiving such patches depends on the CVSS v3.0 Rating:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

The PROJECT_PIVOT team takes security bugs seriously. We appreciate your efforts to responsibly disclose your findings, and will make every effort to acknowledge your contributions.

### How to Report a Security Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to: **[security@projectpivot.com]** (or create a private issue if you prefer)

You should receive a response within 48 hours. If for some reason you do not, please follow up via email to ensure we received your original message.

### What to Include in Your Report

Please include the following information in your report:

- Type of issue (e.g. buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit the issue

### Our Security Measures

PROJECT_PIVOT implements several security measures:

#### Data Protection
- **Camera Data**: All camera data is processed locally on the device
- **No Data Transmission**: Pose data is not transmitted to external servers
- **Local Processing**: All ML inference happens on-device
- **No Data Storage**: Camera frames are not stored persistently

#### Authentication
- **Google Sign-In**: Secure authentication using Google's OAuth 2.0
- **Firebase Security**: User data protected by Firebase security rules
- **No Sensitive Data**: No sensitive personal information is collected

#### Permissions
- **Minimal Permissions**: Only essential permissions are requested
- **Camera Permission**: Required for pose detection functionality
- **Runtime Permissions**: All permissions requested at runtime with user consent

#### Code Security
- **ProGuard/R8**: Code obfuscation enabled in release builds
- **Secure Coding**: Following Android security best practices
- **Dependency Management**: Regular updates of dependencies
- **No Root Access**: App does not require or request root access

### Security Considerations for Users

#### Device Security
- Keep your Android device updated with the latest security patches
- Install the app only from official sources (Google Play Store)
- Review app permissions before installation
- Use device lock screen protection

#### Privacy
- The app processes camera data locally - no video is sent to servers
- User pose data is not stored or transmitted
- Only basic user profile information is stored in Firebase

#### Network Security
- App communicates only with Firebase services for authentication
- All network communications use HTTPS/TLS encryption
- No sensitive data is transmitted over the network

### Vulnerability Disclosure Timeline

1. **Initial Report**: Security issue reported to our team
2. **Acknowledgment**: We acknowledge receipt within 48 hours
3. **Investigation**: We investigate and validate the issue (5-10 business days)
4. **Resolution**: We develop and test a fix (varies based on complexity)
5. **Disclosure**: We coordinate disclosure with the reporter
6. **Release**: Security update is released to users

### Security Updates

Security updates will be released as:
- **Critical**: Immediate patch release
- **High**: Patch within 7 days
- **Medium**: Patch within 30 days
- **Low**: Patch in next regular release

### Scope

This security policy applies to:
- PROJECT_PIVOT Android application
- Related infrastructure (Firebase, authentication)
- Official distribution channels

### Out of Scope

The following are typically out of scope:
- Social engineering attacks
- Physical device attacks
- Issues in third-party dependencies (report to respective maintainers)
- Attacks requiring physical access to unlocked devices

### Recognition

We appreciate security researchers who help keep PROJECT_PIVOT and our users safe. Responsible disclosure of security vulnerabilities helps us ensure the security and privacy of our users.

Security researchers who responsibly disclose security issues will be:
- Acknowledged in our security advisories (if desired)
- Listed in our Hall of Fame (if desired)
- Provided with PROJECT_PIVOT branded merchandise (for significant findings)

### Contact

For security-related questions or concerns, please contact:
- **Email**: [security@projectpivot.com]
- **GitHub**: Create a private security advisory
- **Response Time**: Within 48 hours

### Legal

This security policy is subject to our [Terms of Service](https://projectpivot.com/terms) and [Privacy Policy](https://projectpivot.com/privacy).

---

**Last Updated**: January 2025 