# OCR Extraction Specification

## Clean Text Recognition
- GIVEN a clean scan of a document with printed text
- WHEN the OCR engine recognizes the image region
- THEN the extracted text matches the original with >95% accuracy

## Title Text
- GIVEN an image region containing a title in large font
- WHEN the OCR engine recognizes it
- THEN the full title text is extracted correctly

## Paragraph Text
- GIVEN an image region containing a paragraph of body text
- WHEN the OCR engine recognizes it
- THEN the text is extracted preserving word boundaries
- AND line breaks within a paragraph are normalized to spaces

## Code Text
- GIVEN an image region containing monospaced code
- WHEN the OCR engine recognizes it
- THEN indentation and whitespace are preserved as closely as possible
- AND special characters (braces, brackets, semicolons) are recognized

## Graduated Quality Thresholds
- GIVEN an easy fixture (clean scan)
- THEN OCR accuracy should be >95% (near-exact match)

- GIVEN a medium fixture (decent phone photo)
- THEN key phrases and structure must be recognizable

- GIVEN a hard fixture (poor quality photo)
- THEN at least 50% of expected content should be recognized

## Empty Region
- GIVEN an image region with no text content
- WHEN the OCR engine recognizes it
- THEN it returns an empty or whitespace-only string
