package com.atustcchen.javaailongchain4j.Tools;

import org.springframework.stereotype.Component;

@Component("appointmentTools")
public class AppointmentTools {
//    @Autowired
//    private AppointmentService appointmentService;
//
//    @Tool(name="预约挂号",value="根据参数，先执行工具queryDepartment查询是否可预约，并且告知用户，并让用户确认所有预约信息再预约，如果没有提供医生姓名就从向量数据库中随机挑一个医生即可")
//    public String bookAppointment(Appointment appointment){
//        //看看数据库中是否已经预约了
//        Appointment existAppointment = appointmentService.getOne(appointment);
//        if(existAppointment == null){
//            appointment.setId(null);
//            if(appointmentService.save(appointment)){
//                return "预约成功";
//            }
//            else{
//                return "预约失败";
//            }
//        }
//        return "您已经预约过了，请勿重复预约";
//
//    }
//    @Tool(name="取消预约",value="根据参数，取消预约")
//    public String cancelAppointment(Appointment appointment){
//        //看看数据库中是否已经预约了
//        Appointment existAppointment = appointmentService.getOne(appointment);
//        if(existAppointment!= null){
//            appointmentService.removeById(existAppointment.getId());
//            return "取消预约成功";
//        }
//        return "您尚未预约，无法取消预约";
//    }
//
//    @Tool(name="查询是否有号源",value="根据科室名称、日期、时间和医生姓名查询是否有号源，并返回给用户结果")
//    public boolean queryDepartment(
//            @P(value="科室名称") String departmentName,
//            @P(value="日期") String date,
//            @P(value="时间") String time,
//            @P(value="医生",required = false) String doctorName
//    ){
//        System.out.println("正在查询是否有号源......");
//        System.out.println("科室名称是"+departmentName);
//        System.out.println("日期是"+date);
//        System.out.println("时间是"+time);
//        System.out.println("医生名字是"+doctorName);
//
//        //看看有无医生名字
//        if(doctorName == null || doctorName.isEmpty()){
//            System.out.println("未指定医生姓名");
//            //看看还有没有空闲号
//
//        }
//        else{
//            //看看这个医生下面有几个号
//            LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(Appointment::getDoctor_name, doctorName);
//            long count = appointmentService.count(queryWrapper);
//            if(count<50){
//                System.out.println("医生尚有余号");
//            }
//            else {
//                return false;
//            }
//        }
//        return true;
//    }
}
