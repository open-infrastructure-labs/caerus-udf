package org.openinfralabs.caerus.UdfRegistry.repository;

import org.openinfralabs.caerus.UdfRegistry.model.NewUdf;
import org.openinfralabs.caerus.UdfRegistry.model.Udf;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface UdfDao {
    boolean saveUdf(String newUdf, MultipartFile file);

    List<Udf> fetchAllUdf();

    Udf getUdfById(String id);

    boolean deleteUdfById(String id);

    Resource getFileResourceById(String id);

    boolean updateUdf(String id, String updateUdfConfig);

    boolean updateUdfExecutable(String id, MultipartFile file);
}
