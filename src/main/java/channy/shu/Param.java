package channy.shu;


import java.util.List;

public class Param {
    private String username;
    private String pwd;
    private List<Integer> times;


    public List<Integer> getTimes() {
        return times;
    }

    public String getPwd() {
        return pwd;
    }

    public String getUsername() {
        return username;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void setTimes(List<Integer> times) {
        this.times = times;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
