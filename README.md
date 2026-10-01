# Auto-video-app

GameClip Publisher: phone video selection -> backend clip generation/music -> Google Drive queue -> scheduled social publishing.

## Current progress
- Expo SDK 57 phone app
- Phone video picker with explicit media-library permission
- 3/6/10/15 clip selection
- 15/30/45/60 second selection
- 9:16/landscape option
- Multipart upload uses Expo SDK 57 File + expo/fetch
- Backend processing/publishing architecture is being integrated
- YouTube uploader implemented in the development package
- TikTok/Instagram/Facebook require their official API credentials and permissions

## Latest bug fix
The old DocumentPicker/File path produced a READ-permission error during File.bytes. The phone app now uses expo-image-picker for videos and explicitly requests media-library permission before constructing the upload File.

## Local mobile setup
cd mobile
npm install
npx expo start -c
