package at.ac.meduniwien.vcfvisualize.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class FilterDTO {
    public long offset;
    public List<OrderDTO> order;
    public JsonNode expression;
}