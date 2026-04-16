package com.atustcchen.javaailongchain4j.Bean.login;


public class LoginUserHolder {
    public static ThreadLocal<LoginUser> loginUserHolder = new ThreadLocal<>();
    public static void setLoginUser(LoginUser user) {
        loginUserHolder.set(user);
    }
    public static void clear() {
        loginUserHolder.remove();
    }

    public static LoginUser getLoginUser() {
        return loginUserHolder.get();
    }
}
