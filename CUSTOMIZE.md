# Customizing the Application

## Overview

You may customize the following parts of the application:
- Filterable columns
- Names, descriptions, default- and sample values for variant properties
- The properties displayed on the detail page for a variant
- The order, titles and subtitles of columns displayed on the detail page for a variant
- The list of genes

## Customization

Perform the following steps in order to customize one of the JSON files.

1. Create the directory `~/data/vv/core`.
2. Copy one or all of the json files from [/tool/core/src/main/resources/defaultconfiguration](/tool/core/src/main/resources/defaultconfiguration) to `~/data/vv/core` and adapt it accordingly.

No restart is required after adding the files to `~/data/vv/core`, your changes should come into effect immediately.

### Filterable columns

Go to the [columns.json file](/tool/core/src/main/resources/defaultconfiguration/columns.json) and adapt the JSON array `filterable` by adding or removing ids of variant properties. Note that these ids have to match the SQL column names as stored in the Postgresql database.

### Names, descriptions, default- and sample values

Go to the [columns.json file](/tool/core/src/main/resources/defaultconfiguration/columns.json) and adapt the JSON array `columns`. The `columns' array contains a number of JSON objects each containing information about a variant property.
- `name` describes the displayed name in the frontend.
- `id` has to match the SQL column names as stored in the Postgresql database.
- `link` may contain an HTTP link to a paper or resource further explaining the variant property.
- `description` is a human readable description of the variant property.
- `sample` is sample value the property may take.
- `range` may be added for non numberical columns describing the range of values this property can take.
- `displayable` can be `true` or `false` and defines if the value of the property is displayed in the frontend.

### Properties, order, titles and subtitle displayed on the detail page for a variant

Go to the [view.json file](/tool/core/src/main/resources/defaultconfiguration/view.json) and adapt the JSON array `chapters`. Each chapter can contain arbitrarily nested `subchapters` each with a JSON array `fields` which contains a list of ids as defined by [columns.json](/tool/core/src/main/resources/defaultconfiguration/columns.json). These ids correspond to the variant properties shown on the variant detail page.


