package com.atustcchen.javaailongchain4j.Tools;

import com.atustcchen.javaailongchain4j.feign.queryFeign;
import com.atustcchen.javaailongchain4j.req.PassengerQueryReq;
import com.atustcchen.javaailongchain4j.resp.CommonResp;
import com.atustcchen.javaailongchain4j.resp.PageResp;
import com.atustcchen.javaailongchain4j.resp.PassengerQueryResp;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("apartmentTools")
public class ApartmentTools {

    @Autowired
    private queryFeign queryFeign;

    @Tool(name="查询乘车人信息",value="查询乘车人信息，并且告诉用户")
    public CommonResp<PageResp<PassengerQueryResp>> queryPassengerInfo(){
        log.info("查询乘车人信息");
        PassengerQueryReq req = new PassengerQueryReq();
        req.setPage(1);
        req.setSize(10);
        CommonResp<PageResp<PassengerQueryResp>> result = queryFeign.queryList(req);
        return result;
    }

//    @Tool(name="根据id获取公寓详细信息",value="根据用户获得的详细id来查询公寓的详细信息")
//    public ApartmentDetailVo getApartmentInfoById(
//            @P(value="公寓id") Long id
//    ){
//        ApartmentDetailVo result = apartmentInfoService.getDetailById(id);
//        return result;
//    }


}
