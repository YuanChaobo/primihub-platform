package com.primihub.application.controller.data;

import com.primihub.biz.entity.base.BaseResultEntity;
import com.primihub.biz.entity.base.BaseResultEnum;
import com.primihub.biz.entity.data.po.DataPsi;
import com.primihub.biz.entity.data.po.DataPsiTask;
import com.primihub.biz.entity.data.req.DataPsiReq;
import com.primihub.biz.entity.data.req.DataPsiResourceReq;
import com.primihub.biz.entity.data.req.DataResourceReq;
import com.primihub.biz.entity.data.req.PageReq;
import com.primihub.biz.service.data.DataPsiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.UUID;

@RequestMapping("psi")
@RestController
@Slf4j
public class PsiController {

    @Autowired
    private DataPsiService dataPsiService;

    @PostMapping("saveDataPsi")
    public BaseResultEntity saveDataPsi(@RequestHeader("userId") Long userId,
                                        DataPsiReq req){
        if (userId<=0)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"userId");
        if (req.getOwnOrganId()==null)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"ownOrganId");
        if (req.getOwnResourceId()==null)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"ownResourceId");
        if (StringUtils.isBlank(req.getOwnKeyword()))
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"ownKeyword");
        if (req.getOtherOrganId()==null)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"otherOrganId");
        if (req.getOtherResourceId()==null)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"otherResourceId");
        if (StringUtils.isBlank(req.getOtherKeyword()))
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"otherKeyword");
        if (StringUtils.isBlank(req.getResultName()))
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"resultName");
        if (StringUtils.isBlank(req.getResultOrganIds()))
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"resultOrganIds");
        if (req.getPsiTag()==null)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"psiTag");
        return dataPsiService.saveDataPsi(req,userId);
    }

    @GetMapping("getPsiResourceList")
    public BaseResultEntity getPsiResourceList(DataResourceReq req, Long organId){
        if (organId==null||organId==0L)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"organId");
        return dataPsiService.getPsiResourceList(req,organId);
    }

    @GetMapping("getPsiResourceAllocationList")
    public BaseResultEntity getPsiResourceDataList(PageReq req,
                                                   String organId,
                                                   String serverAddress,
                                                   String resourceName){
//        if (StringUtils.isBlank(organId))
//            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"organId");
        return dataPsiService.getPsiResourceAllocationList(req,organId,serverAddress,resourceName);
    }

    @GetMapping("getPsiTaskList")
    public BaseResultEntity getPsiTaskList(String resultName,
                                           PageReq req){
        return  dataPsiService.getPsiTaskList(req,resultName);
    }
    @GetMapping("getOrganPsiTask")
    public BaseResultEntity getOrganPsiTask(@RequestHeader("userId") Long userId,
                                            String resultName,
                                            PageReq req){
        if (userId<=0)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"userId");
        return dataPsiService.getOrganPsiTask(userId,resultName,req);
    }

    @GetMapping("getPsiTaskDetails")
    public BaseResultEntity getPsiTaskDetails(@RequestHeader("userId") Long userId,
                                               Long taskId){
        if (userId<=0)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"userId");
        if (taskId==null||taskId==0L)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"taskId");
        return  dataPsiService.getPsiTaskDetails(taskId,userId);
    }

    @GetMapping("downloadPsiTask")
    public void downloadPsiTask(HttpServletResponse response,Long taskId) throws Exception{
        if (taskId==null||taskId==0L)
            return;
        DataPsiTask task = dataPsiService.selectPsiTaskById(taskId);
        DataPsi dataPsi = dataPsiService.selectPsiById(task.getPsiId());
        File file = new File(task.getFilePath());
        if (file!=null&&file.exists()){
            String fileName = dataPsi.getResultName()+".csv";
            FileInputStream inputStream = new FileInputStream(file);
            response.setHeader("content-Type","application/vnd.ms-excel");
            response.setHeader("content-disposition", "attachment; fileName=" + new String(fileName.getBytes("UTF-8"),"iso-8859-1"));
            ServletOutputStream outputStream = response.getOutputStream();
            int len = 0;
            byte[] data = new byte[1024];
            while ((len = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, len);
            }
            outputStream.close();
            inputStream.close();
        }else {
            String content = "no data";
            String fileName = null;
            if (dataPsi!=null){
                if (task.getFileContent()!=null){
                    content = task.getFileContent();
                }
                fileName = dataPsi.getResultName()+".csv";
            }else {
                fileName = UUID.randomUUID().toString()+".csv";
            }
            OutputStream outputStream = null;
            //将字符串转化为文件
            byte[] currentLogByte = content.getBytes();
            try {
                // 告诉浏览器用什么软件可以打开此文件
                response.setHeader("content-Type","application/vnd.ms-excel");
                // 下载文件的默认名称
                response.setHeader("Content-disposition","attachment;filename="+ new String(fileName.getBytes("UTF-8"),"iso-8859-1"));
                response.setCharacterEncoding("UTF-8");
                outputStream = response.getOutputStream();
                outputStream.write(currentLogByte);
                outputStream.close();
                outputStream.flush();
            }catch (Exception e) {
                log.info("downloadPsiTask -- fileName:{} -- fileContent:{} -- e:{}",fileName,content,e.getMessage());
            }
        }
    }

    @GetMapping("delPsiTask")
    public BaseResultEntity delPsiTask(@RequestHeader("userId") Long userId,
                                              Long taskId){
        if (taskId==null||taskId==0L)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"taskId");
        return  dataPsiService.delPsiTask(taskId,userId);
    }

    @GetMapping("cancelPsiTask")
    public BaseResultEntity cancelPsiTask(@RequestHeader("userId") Long userId,
                                       Long taskId){
        if (taskId==null||taskId==0L)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"taskId");
        return  dataPsiService.cancelPsiTask(taskId,userId);
    }

    @GetMapping("retryPsiTask")
    public BaseResultEntity retryPsiTask(@RequestHeader("userId") Long userId,
                                          Long taskId){
        if (taskId==null||taskId==0L)
            return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"taskId");
        return  dataPsiService.retryPsiTask(taskId,userId);
    }


}
