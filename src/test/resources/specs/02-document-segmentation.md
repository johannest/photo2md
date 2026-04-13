# Document Segmentation Specification

## Title Detection
- GIVEN a preprocessed image containing a large-font heading
- WHEN the segmenter analyzes it
- THEN it identifies a Title element with appropriate level
- AND the bounding box covers the heading region

## Paragraph Detection
- GIVEN a preprocessed image containing body text
- WHEN the segmenter analyzes it
- THEN it identifies one or more Paragraph elements
- AND the bounding boxes do not overlap

## List Detection
- GIVEN a preprocessed image containing bulleted or numbered items
- WHEN the segmenter analyzes it
- THEN it identifies a ListBlock element
- AND the type reflects ordered vs unordered

## Table Detection
- GIVEN a preprocessed image containing a grid of cells with borders
- WHEN the segmenter analyzes it
- THEN it identifies a Table element
- AND the bounding box covers the entire table region

## Code Block Detection
- GIVEN a preprocessed image containing monospaced text in a distinct background region
- WHEN the segmenter analyzes it
- THEN it identifies a CodeBlock element

## Image Region Detection
- GIVEN a preprocessed image containing an embedded photograph or diagram
- WHEN the segmenter analyzes it
- THEN it identifies an ImageRegion element

## Element Ordering
- GIVEN a preprocessed image with multiple element types
- WHEN the segmenter analyzes it
- THEN elements are returned ordered top-to-bottom by their vertical position
