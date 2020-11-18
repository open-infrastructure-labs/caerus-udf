package org.openinfralabs.caerus.UdfRegistry.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.openinfralabs.caerus.UdfRegistry.model.NewUdf;
import org.openinfralabs.caerus.UdfRegistry.model.Udf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class UdfDaoImpl implements UdfDao {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String KEY = "UDF";
    private static final String KEY_FILE = "UDF_FILE";

    @Override
    public boolean saveUdf(String newUdfStr, MultipartFile file) {

        try {
            // Creating a random UUID (Universally unique identifier).

            // TODO: Need a decision on if the ID is really needed, it is probably enough to just use name as a key
            //UUID uuid = UUID.randomUUID();
            //String randomUUIDString = uuid.toString();

            ObjectMapper mapper = new ObjectMapper();
            NewUdf newUdf = mapper.readerFor(NewUdf.class).readValue(newUdfStr);

            Udf udf = new Udf();
            udf.setId(newUdf.getName());
            udf.setName(newUdf.getName());
            udf.setInterfaceVersion(newUdf.getInterfaceVersion());
            udf.setLanguage(newUdf.getLanguage());
            udf.setLanguageVersion(newUdf.getLanguageVersion());
            udf.setMain(newUdf.getMain());
            udf.setPkg(newUdf.getPkg());
            udf.setFileName(file.getOriginalFilename());
            udf.setInvocationEvents(newUdf.getInvocationEvents());
            redisTemplate.opsForHash().put(KEY, newUdf.getName(), udf);
            // use same uuid as key of file table
            // TODO: for large file, we should save it to the file system, and only save the path here, load it when needed
            //       also redis have max size of 512 MB, it should be enough for most of the udfs, but it is still better
            //       to have a second mechanism

            // redis should be binary safe for key or value, use getBytes directly
            redisTemplate.opsForHash().put(KEY_FILE, newUdf.getName(), file.getBytes());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Udf> fetchAllUdf() {
        List<Udf> udfs;
        udfs = redisTemplate.opsForHash().values(KEY);
        return udfs;
    }


    @Override
    public Udf getUdfById(String id) {
        Udf udf = (Udf)redisTemplate.opsForHash().get(KEY, id);
        return udf;
    }

    @Override
    public boolean deleteUdfById(String id) {
        try {
            redisTemplate.opsForHash().delete(KEY, id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public Resource getFileResourceById(String id) {

        byte[] valueBytes = (byte[])redisTemplate.opsForHash().get(KEY_FILE, id);
        ByteArrayResource resource = new ByteArrayResource(valueBytes);
        return resource;
    }

    @Override
    public boolean updateUdf(String id, String updateUdfConfig) {

        try {

            Udf existingUdf = (Udf)redisTemplate.opsForHash().get(KEY, id);

            ObjectMapper mapper = new ObjectMapper();
            ObjectReader objectReader = mapper.readerForUpdating(existingUdf);
            Udf updateUdf = objectReader.readValue(updateUdfConfig);
            redisTemplate.opsForHash().put(KEY, existingUdf.getId(), updateUdf);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateUdfExecutable(String id, MultipartFile file) {
        try {

            Udf existingUdf = (Udf)redisTemplate.opsForHash().get(KEY, id);

            redisTemplate.opsForHash().put(KEY_FILE, existingUdf.getId(), file.getBytes());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
