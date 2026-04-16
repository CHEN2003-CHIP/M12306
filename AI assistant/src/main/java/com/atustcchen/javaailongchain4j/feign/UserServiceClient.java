//package com.atustcChen.feign;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//@FeignClient(name = "user-service")  // 要调用的服务名称
//public interface UserServiceClient {
//
//    @GetMapping("/api/users/{id}")
//    UserDTO getUserById(@PathVariable("id") Long id);
//
//    @PostMapping("/api/users")
//    UserDTO createUser(@RequestBody UserCreateRequest request);
//
//    // 响应DTO
//    record UserDTO(Long id, String username, String email) {}
//
//    // 请求DTO
//    record UserCreateRequest(String username, String email, String password) {}
//}
