package me.zxhm.map.fly;

public class GaussKruegerConverter {
    static double a = 6378137;
    static double b = 6356752.3142;
    static double k0  = 1.0;

    static double f = (a - b) / a;//扁率
    static double e = Math.sqrt(2*f - Math.pow(f ,2));//第一偏心率
    static double e1 = e / Math.sqrt(1 - Math.pow(e , 2));//'第二偏心率

    /**
     * 经纬度转平面直角坐标系
     * @param lng
     * @param lat
     * @return
     */
    public static double []  LngLat2XY(double lng,double lat)
    {
        int centerLongitude = (int)Math.round(lng/3.0) * 3;
        double BR = lat * Math.PI / 180;//纬度弧长
        double lo = (lng - centerLongitude)*Math.PI/180; //经差弧度
        double N = a / Math.sqrt(1 - Math.pow((e * Math.sin(BR)) , 2)); //卯酉圈曲率半径

        //求解参数s
        double C = Math.pow(a , 2)/ b;
        double B0 = 1 - 3 * Math.pow(e1 , 2) / 4 + 45 *Math.pow( e1 ,4) / 64 - 175 * Math.pow(e1 , 6) / 256 + 11025 * Math.pow(e1 , 8 )/ 16384;
        double B2 = B0 - 1;
        double B4 = 15 / 32 * Math.pow(e1 , 4) - 175 / 384 * Math.pow(e1 , 6 )+ 3675 / 8192 *Math.pow( e1 , 8);
        double B6 = 0 - 35 / 96 *Math.pow( e1 , 6) + 735 / 2048 * Math.pow(e1 , 8);
        double B8 = 315 / 1024 * Math.pow(e1 , 8);
        double s = C * (B0 * BR + Math.sin(BR) * (B2 * Math.cos(BR) + B4 * Math.pow((Math.cos(BR)) , 3) + B6 * Math.pow((Math.cos(BR)) , 5 )+ B8 * Math.pow((Math.cos(BR)) , 7)));

        double t = Math.tan(BR);
        double g = e1 * Math.cos(BR);

        double XR= s + Math.pow(lo , 2) / 2 * N * Math.sin(BR) * Math.cos(BR) + Math.pow(lo , 4 )* N * Math.sin(BR) * Math.pow((Math.cos(BR)) , 3) / 24 * (5 -Math.pow( t , 2 )+ 9 * Math.pow(g , 2) + 4 *Math.pow( g , 4)) + Math.pow(lo , 6) * N * Math.sin(BR) * Math.pow((Math.cos(BR)) , 5) * (61 - 58 *Math.pow( t , 2) + Math.pow(t , 4)) / 720;
        double YR= lo * N * Math.cos(BR) + Math.pow(lo , 3 )* N / 6 *Math.pow( (Math.cos(BR)) , 3) * (1 - Math.pow(t , 2) + Math.pow(g , 2)) + Math.pow(lo , 5) * N / 120 * Math.pow((Math.cos(BR)) , 5) * (5 - 18 * Math.pow(t , 2) + Math.pow(t , 4) + 14 * Math.pow(g , 2) - 58 * Math.pow(g , 2) * Math.pow(t , 2));
        double x=YR;
        double y=XR;
        return new double[]{x,y};
    }

    public static double[] XY2LngLat(double x,double y,int centerLongitude)
    {
        double El1 = (1 - Math.sqrt(1 - Math.pow(e , 2))) / (1 + Math.sqrt(1 -Math.pow( e , 2)));

        double Mf = y/ k0 ;//真实坐标值

        double Q = Mf / (a * (1 - Math.pow(e , 2) / 4 - 3 * Math.pow(e , 4) / 64 - 5 *Math.pow( e , 6) / 256));//角度

        double Bf = Q + (3 * El1 / 2 - 27 *Math.pow( El1 , 3) / 32) * Math.sin(2 * Q) + (21 *Math.pow( El1 , 2) / 16 - 55 *Math.pow( El1 , 4 )/ 32) * Math.sin(4 * Q) + (151 *Math.pow( El1 , 3 )/ 96) * Math.sin(6 * Q) + 1097 / 512 * Math.pow(El1 , 4) * Math.sin(8 * Q);
        double Rf = a * (1 -Math.pow( e , 2)) / Math.sqrt(Math.pow((1 - Math.pow((e * Math.sin(Bf)) ,2)) , 3));
        double Nf = a / Math.sqrt(1 - Math.pow((e * Math.sin(Bf)) , 2));//卯酉圈曲率半径
        double Tf = Math.pow((Math.tan(Bf)) , 2);
        double D =x/ (k0 * Nf);

        double Cf =Math.pow( e1 , 2) * Math.pow((Math.cos(Bf)) , 2);

        double B = Bf - Nf * Math.tan(Bf) / Rf * (Math.pow(D , 2) / 2 - (5 + 3 * Tf + 10 * Cf - 9 * Tf * Cf - 4 *Math.pow( Cf , 2) - 9 * Math.pow(e1 , 2)) *Math.pow( D , 4) / 24 + (61 + 90 * Tf + 45 * Math.pow(Tf , 2) - 256 * Math.pow(e1 , 2) - 3 * Math.pow(Cf , 2)) *Math.pow( D , 6) / 720);
        double L = centerLongitude*Math.PI/180 + 1 / Math.cos(Bf) * (D - (1 + 2 * Tf + Cf) *Math.pow( D , 3) / 6 + (5 - 2 * Cf + 28 * Tf - 3 *Math.pow( Cf , 2) + 8 * Math.pow(e1 , 2) + 24 * Math.pow(Tf , 2)) * Math.pow(D , 5 )/ 120);

        double Bangle = B * 180 / Math.PI;
        double Langle = L * 180 / Math.PI;

        return new double[]{Langle,Bangle};
    }

}
