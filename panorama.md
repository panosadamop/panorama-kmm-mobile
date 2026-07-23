# Mobile App Specification — panoramapolihnitou.gr

## 1. Overview

Mobile application for the website panoramapolihnitou.gr.

The application consumes content from the WordPress REST API and provides:

- Home page with featured slider
- Main posts as cards
- Article list view
- Full article details
- Categories navigation
- Static pages
- Article bookmarking
- Social media sharing

---

# 2. Technology Stack

## Recommended Technologies

### Option A (Recommended)
- Flutter
- Dart
- REST API integration
- Local storage for bookmarks

### Option B
- React Native
- TypeScript

### Option C
- Kotlin Multiplatform

---

# 3. Backend / CMS

## CMS

WordPress

## REST API Base URL

```text
https://panoramapolihnitou.gr/wp-json/wp/v2/