package org.openinfralabs.caerus.ndpService.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openinfralabs.caerus.ndpService.service.StorageAdapter;
import org.openinfralabs.caerus.ndpService.service.StorageAdapterMinioImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    // This is good to have for debug and figure out what exactly the raw request that sent by a client
    // like aws sdk or minio client sdk
    @Autowired
    StorageAdapter adapter;

    Logger logger = LoggerFactory.getLogger(AdminInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        logger.info("\n-------- AdminInterceptor.preHandle --- ");

       // Getting servlet request URL: example to show detail s3 request content for debugging
 /*       String url = request.getRequestURL().toString();

        // Getting servlet request query string.
        String queryString = request.getQueryString();

        // Getting request information without the hostname.
        String uri = request.getRequestURI();

        // Below we extract information about the request object path
        // information.
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int portNumber = request.getServerPort();
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String query = request.getQueryString();


        System.out.println("Url: " + url + "<br/>");
        System.out.println("Uri: " + uri + "<br/>");
        System.out.println("Scheme: " + scheme + "<br/>");
        System.out.println("Server Name: " + serverName + "<br/>");
        System.out.println("Port: " + portNumber + "<br/>");
        System.out.println("Context Path: " + contextPath + "<br/>");
        System.out.println("Servlet Path: " + servletPath + "<br/>");
        System.out.println("Path Info: " + pathInfo + "<br/>");
        System.out.println("Query: " + query);


        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                System.out.println("Header: " + request.getHeader(headerNames.nextElement()));
            }
        }


        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {

            String paramName = parameterNames.nextElement();
            System.out.println(paramName);
            System.out.println(":");

            String[] paramValues = request.getParameterValues(paramName);
            for (int i = 0; i < paramValues.length; i++) {
                String paramValue = paramValues[i];
                System.out.println(paramValue);
            }

        }
        String method = request.getMethod();
        InputStream inputStream =  request.getInputStream();
        String bucket = "imagesbucket";
        String fileName = "imagesbucket";
        UdfInvocationMetadata metadataObj = new UdfInvocationMetadata();
        metadataObj.setName("caerus-faas-spring-thumbnail");
        List<String> inputParameters = new ArrayList<String> ();
        inputParameters.add("400");
        inputParameters.add("600");
        metadataObj.setInputParameters(inputParameters);
      //  adapter.uploadFile(bucket, fileName, inputStream, metadataObj);

        /*Map<String, String> result = new HashMap<>();
        result.put("key", filename);
        return result;*/


        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, //
                           Object handler, ModelAndView modelAndView) throws Exception {

        logger.info("\n-------- AdminInterceptor.postHandle --- ");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, //
                                Object handler, Exception ex) throws Exception {

        logger.info("\n-------- AdminInterceptor.afterCompletion --- ");
    }

}

