package model;

public class ErrorMessage {
    public String msg;
    public String desc;

    public ErrorMessage(String msg) {
        this.msg = msg;
    }

    public ErrorMessage(String msg, String desc) {
        this.msg = msg;
        this.desc = desc;
    }
}
