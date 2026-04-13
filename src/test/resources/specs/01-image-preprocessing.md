# Image Preprocessing Specification

## Grayscale Conversion
- GIVEN a color photograph of a document
- WHEN the preprocessor processes it
- THEN the output image is grayscale (single channel or equivalent)
- AND the output dimensions match the input dimensions (within 5% tolerance for deskew)

## Contrast Enhancement
- GIVEN a low-contrast grayscale document image
- WHEN the preprocessor applies contrast enhancement
- THEN the output has higher contrast between text and background
- AND text regions appear darker relative to the background

## Null Safety
- GIVEN a null or empty byte array
- WHEN the preprocessor is called
- THEN it throws an IllegalArgumentException

## Format Support
- GIVEN an image in PNG format
- WHEN the preprocessor processes it
- THEN it returns a valid preprocessed image

- GIVEN an image in JPEG format
- WHEN the preprocessor processes it
- THEN it returns a valid preprocessed image
