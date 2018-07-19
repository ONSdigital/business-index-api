# Bulk Match with StreamSets

Data drift pipeline for Business Index(BI) API.

## Description

Primary goal of pipeline is to collect in bulks BI records based on requested parameters.

User can send CSV files with X requests. Pipeline is processing it in concurrent mode and generate output CSV file with responses based on requested params.

Possible values of requested params:

- Business Index fields(BusinessName, VAT, PAYE, Industry Code, etc.).
- Elastic search eligible query.


## Notes

### Configuration

Template for pipeline provided. It contains set of ***_VAR placeholders.
Each placeholder must be replaced with real value before submitting it to Streamsets instance.

### Performance

Based on elasticsearch/business api performance parallelism and/or throttling should be adjusted.
All this configuration is located in BI-API (HTTP Client) component.

### Input data
One column CSV with header.


### Exception handling 

If header in CSV file is unknown - file is ignored.

If there are no results for particular record - record redirected to err output.

If exception happened for some particular record - it redirected to err output.

If request is too wide: there are more than 10 BI responses for it (value is configurable) - it redirected to err output (no data retrieved).

### Output file(s)

Results of StreamSets pipeline is the list of files. One file with *out* suffix, another with *err*, if at least one record went through exception flow.
  
Maximum size of file is configurable, if there are more records than expected - files wiwth suffix *.1*, *.2* etc. will be created.
