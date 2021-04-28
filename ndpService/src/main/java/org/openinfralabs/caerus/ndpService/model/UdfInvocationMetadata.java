package org.openinfralabs.caerus.ndpService.model;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;


@Entity
public class UdfInvocationMetadata implements Serializable {

    private String name;
    /*
     *  Optional: provide input parameters for the UDF function
     */
    @Column(nullable = true)
    private List<String> inputParameters;
    /*
     *  Optional: provide keys of the objects that this UDF needs operate on (UDFs with multi-object operation)
     */
    @Column(nullable = true)
    private List<String> extraResources;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<List<String>> getInputParameters() {
        return Optional.ofNullable(inputParameters);
    }

    public void setInputParameters(List<String> inputParameters) {

        this.inputParameters = inputParameters;
    }

    public Optional<List<String>> getExtraResources() {
        return Optional.ofNullable(extraResources);
    }

    public void setExtraResources(List<String> extraResources) {
        this.extraResources = extraResources;
    }
}
