package org.openinfralabs.caerus.UdfRegistry.service;

import org.openinfralabs.caerus.UdfRegistry.model.NewUdf;
import org.openinfralabs.caerus.UdfRegistry.model.Udf;
import org.openinfralabs.caerus.UdfRegistry.repository.UdfDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
public class UdfSeerviceImpl implements UdfService {

    @Autowired
    private UdfDao udfDao;

    @Override
    public List<Udf> fetchAllUdf() {
       List<Udf> udfs;
       udfs = udfDao.fetchAllUdf();
       return udfs;
    }

    @Override
    public boolean saveUdf(String newUdf, MultipartFile file) {

        return udfDao.saveUdf(newUdf, file);
    }

    @Override
    public Udf getUdfById(String id) {
        return udfDao.getUdfById(id);
    }

    @Override
    public boolean deleteUdfById(String id) {
        return udfDao.deleteUdfById(id);
    }

    @Override
    public Resource getFileResourceById(String id) {
        return udfDao.getFileResourceById(id);
    }

    @Override
    public boolean updateUdf(String id, String updateUdfConfig) {
        return udfDao.updateUdf(id, updateUdfConfig);
    }


    @Override
    public boolean updateUdfExecutable(String id, MultipartFile file) {
        return udfDao.updateUdfExecutable(id, file);
    }

}
