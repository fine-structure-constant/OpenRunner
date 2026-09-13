package cn.edu.pku.pkurunner.Network.Model;

import io.reactivex.Observable;

public class AMapReverseEncoding {
    private String info;
    private String infocode;
    private RegeocodeBean regeocode;
    private String status;

    public static class RegeocodeBean {
        private AddressComponentBean addressComponent;

        public static class AddressComponentBean {
            private String adcode;
            private String citycode;
            private String country;
            private String district;
            private String province;
            private StreetNumberBean streetNumber;
            private String towncode;
            private String township;

            public static class StreetNumberBean {
                private String direction;
                private String distance;
                private String location;
                private String number;
                private String street;
            }
        }
    }

    public Observable<String> getStreetName() {
        return Observable.just(this.regeocode.addressComponent.streetNumber.street);
    }
}
