# UI Upload and Preview Specification

## Page Load
- GIVEN a user navigates to the root URL "/"
- THEN the page displays a heading "Photo to Markdown"
- AND the page displays an upload component accepting image files

## File Upload
- GIVEN the upload component is visible
- WHEN the user uploads a valid image file (PNG or JPEG, < 20MB)
- THEN a progress indicator appears during processing
- AND upon completion, the Markdown editor and preview become visible

## Markdown Editor
- GIVEN a successful conversion
- THEN the editor (TextArea, id: markdown-editor) contains the generated Markdown
- AND the editor is editable

## Live Preview
- GIVEN the Markdown editor contains text
- THEN the preview panel (id: markdown-preview) renders the Markdown as HTML
- WHEN the user edits the Markdown text
- THEN the preview updates to reflect the changes

## Download
- GIVEN a successful conversion with Markdown content
- THEN a download button (id: download-button) is visible
- WHEN the user clicks the download button
- THEN a .md file is downloaded containing the editor content

## File Validation
- GIVEN the upload component
- WHEN the user attempts to upload a file > 20MB
- THEN the upload is rejected with an error message
- AND no processing is triggered

## Mobile PWA
- GIVEN the app is accessed from a mobile device
- THEN the upload component offers camera capture (native browser behavior)
- AND the layout is responsive and usable on small screens
