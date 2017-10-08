package io.emi.service;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang.IncompleteArgumentException;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.DoubleRange;
import org.apache.commons.lang.time.DateUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() {
    Router router = Router.router(vertx);

    router.get("/").handler(this::index);
    router.get("/calculate").handler(this::calculate);
    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(8082);
  }


  private void index(RoutingContext rc){
    rc.response()
      .putHeader("content-type", "text/html")
      .end("<h1>EMI CALCULATOR</h1>");
  }

  private void calculate(RoutingContext rc){

    String principle = rc.request().getParam("principle");
    String rate = rc.request().getParam("rate");
    String term = rc.request().getParam("term");
    String startDate = rc.request().getParam("startDate");
    String repaymentDate = rc.request().getParam("repaymentDate");

    Double Principle = ( principle == null )? 0.0 : Double.parseDouble(principle);
    Double Rate = (rate == null) ? 0.0: Double.parseDouble(rate);
    int Term = (term == null) ? 0: Integer.parseInt(term);
    int RepayDay = ( repaymentDate == null)? 3: Integer.parseInt(repaymentDate);
    try{
      Date StartDate = DateUtils.parseDate(startDate, new String[]{ "yyy/MM/dd"});
      double EMI = this.getEMI(Principle, Rate, Term);
      System.out.print(" EMI: "+EMI+ " \n");
      ArrayList< Payment > payments = this.generatePayments(StartDate, RepayDay, Term, Rate, EMI );
      System.out.print( payments);
      String data = new Gson().toJson(payments);

      rc.response()
        .putHeader("content-type", "application/json; charset:utf-8")
        .end(Json.encodePrettily(data));
    }catch (ParseException e){
      System.out.print(e.getStackTrace());
      rc.response()
        .setStatusCode(500)
        .end(" Unable to Process the request "+e.getStackTrace());

    }
  }

  public static long getDifferenceDays(Date d1, Date d2) {
    long diff = d2.getTime() - d1.getTime();
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
  }

  private ArrayList< Payment > generatePayments( Date startDate, int dayOfRepayment, int term ,double rate,double EMI ){
      ArrayList< Payment > payments = new ArrayList<>();
      Date current = startDate;
      for( int x = 0 ; x <= term-1; x++){

        Date nextMonth = DateUtils.addMonths(current, 1);
        nextMonth.setDate(dayOfRepayment);

        long numDays = this.getDifferenceDays( startDate, nextMonth );

        //        calculating the DF and DCF
        double df = this.getDiscountFactor(rate, numDays);
        System.out.print(" DF IS "+df+ " for Rate: "+rate+ "  Days: "+numDays+" \n");
        double dcf = this.getDiscountedCashFlow(EMI, df);

        payments.add( new Payment(nextMonth, numDays, EMI, df, dcf));
        current = nextMonth;
      }
    return payments;
  }

  private Double getEMI( Double p, Double r, int term ){
      System.out.print( " [*]  Principle : " + p+" Rate: "+r+" Term: "+term + " \n");
      double i =  (r*100) / (12 * 100);
      System.out.print(" Annualized Rate: "+ i+" \n");
      return ((p * i) * Math.pow((1 + i),term))  / (Math.pow((1 + i), term) - 1);
  }

  private double getDiscountFactor(double rate, long days){
    double volume = 15.34;
    double fraction1 = (double) -1/3;
    double cubeSide = Math.pow(volume,fraction1);
    System.out.println("*************************"+cubeSide);


    days = (int)days;
    System.out.print(" ---------------------------------------- \n");
    System.out.println(" - Days: "+days+" Rate : "+rate);

    double fraction = (double) -days/360;
    System.out.print(" Fraction : "+fraction);
    System.out.print(" ---------------------------------------- \n");

    return Math.pow( (1.0 + rate), fraction);
  }

  private Double getDiscountedCashFlow(double emi , double df ){
    return ( emi * df );
  }

}
