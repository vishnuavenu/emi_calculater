package io.emi.service;

import java.util.Date;

/**
 * Created by neo on 10/7/17.
 */
public class Payment {
  private Date  date;
  private Double emi;
  private Double df;
  private Double dcf;
  private long days;

  public Payment(Date date, long days , Double emi, double df, Double dcf ){
      this.date = date;
      this.emi = emi;
      this.df = df;
      this.dcf = dcf;
      this.days = days;
  }
}
