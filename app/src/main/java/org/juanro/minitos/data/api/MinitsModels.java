package org.juanro.minitos.data.api;

import java.util.List;

@SuppressWarnings("unused")
public class MinitsModels {

    public static class Vehicle {
        public String MATRICULA;
        public String MAC;
        public String MAP_MODELO;
        public String LAT;
        public String LON;
        public String MAP_BATERIA;
        public double BATERIA;
        public String MAP_VEHICLEBATTERYKMS;
        public double MAP_IMPORTETARIFA_ACTIVA;
        public double MAP_IMPORTETARIFA_STANDBY;
        public String MAP_ESTADO;
        public String ESTADO;
        public String MAP_COLOR;
        public String MAP_VEHICLEID;
        public String MAP_VEHICLEDESCRIPTION;
        public int MAP_VEHICLEPASSENGERS;
        public String MAP_VEHICLEENGINE;
        public String MAP_VEHICLETRANSMISSION;
        public String MAP_DIRECCION;
    }

    public static class AuthResponse {
        public String TOKEN;
        public String EMAIL;
        public String NOMBRE;
        public int CONDUCIROK;
        public int ACTIVO;
        public String DIRECCION;
        public String CP;
        public String POBLACION;
        public String FECHANAC;
        public int IDSEDE;
        public String TELEFONO;
        public String PREFIJO;
    }

    public static class DatosSQLRequest {
        public String entidad;
        public List<String> where;

        public DatosSQLRequest(String entidad, List<String> where) {
            this.entidad = entidad;
            this.where = where;
        }
    }

    public static class GenericResponse<T> {
        public boolean hasError;
        public String message;
        public String errorCode;
        public T data;
    }

    public static class AccountStatusData {
        public Integer IDUSER;
        public boolean HASERROR;
        public String CODIGOUSUARIO;
        public String PASSUSER;
        public Double TOTAL;
        public Integer VERIFICADO;
        public Integer CONFIRMADO;
        public Integer ACTIVO;
        public Integer IDSEDE;
        public String MENSAJEHORARIO;
    }

    public static class AccountStatusResponse extends GenericResponse<AccountStatusData> {}

    public static class Sede {
        public int ID;
        public String DESCRIPCION;
        public double LATITUD;
        public double LONGITUD;
        public String CORREOAVISO;
        public String TELEFONOAVISO;
        public String TERMINOS;
        public String POLITICA;
        public String ENCODEBOUNDS;
        public String MENSAJEHORARIO;
    }

    public static class SedesResponse extends GenericResponse<Sede[]> {}

    public static class VehiclesResponse extends GenericResponse<Vehicle[]> {}

    public static class ReservationData {
        public int ID;
        public String NUMERORESERVA;
        public String FECHA_INICIO;
        public String HORA_INICIO;
        public String USUARIO;
        public String COCHE;
        public double TARIFA;
        public double TARIFASTANDBY;
        public int IDSEDE;
    }

    public static class ReservationResponse extends GenericResponse<ReservationData> {}

    public static class InfoConducirData {
        public String NUMERORESERVA;
        public String MATRICULA;
        public int ARRANQUE;
        public int FRENOMANO;
        public String TIPO;
        public String MAP_VEHICLEBATTERYKMS;
        public double DEPOSITO;
    }

    public static class InfoConducirResponse extends GenericResponse<InfoConducirData> {}

    public static class VehiculoArrancadoData {
        public String FECHAHORAACTUAL;
        public int ARRANCADO;
        public String FECHA;
    }

    public static class VehiculoArrancadoResponse extends GenericResponse<VehiculoArrancadoData[]> {}

    public static class LoginResponseData {
        public String TOKEN;
    }

    public static class LoginResponse extends GenericResponse<LoginResponseData> {}

    public static class ZonePoint {
        public String latitude;
        public String longitude;
    }

    public static class Zone {
        public int id;
        public String fillcolor;
        public String color;
        public int width;
        public String pattern;
        public List<ZonePoint> data;
    }

    public static class ZonesResponse extends GenericResponse<Zone[]> {}

    public static class Parking {
        public int ID;
        public String NOMBRE;
        public String LAT;
        public String LON;
        public String DIRECCION;
        public String TIPO; // P (Public), R (Reserved), etc.
    }

    public static class ParkingsResponse extends GenericResponse<Parking[]> {}

    public static class GeneralData {
        public int ID;
        public int ACTIVO;
        public String MENSAJEHORARIO;
        public int TIEMPORESERVA;
        public String CORREOAVISO;
        public String TELEFONOAVISO;
        public String TERMINOS;
        public String POLITICA;
        public String ENCODEBOUNDS;
        public String ANDROIDLICENSE;
        public String FUC;
        public String TERMINAL;
        public String CURRENCY;
    }

    public static class GeneralDataResponse extends GenericResponse<GeneralData[]> {}

    public static class FinalizarConduccionData {
        public int ID;
        public String NUMERORESERVA;
        public String FECHA_INICIO;
        public String HORA_INICIO;
        public String FECHA_FIN;
        public String HORA_FIN;
        public String USUARIO;
        public String COCHE;
        public double TARIFA;
        public double TARIFASTANDBY;
        public double COSTETOTAL;
        public double COSTE_INICIAL;
        public double COSTE_STANDBY;
        public double COSTE_CONDUCCION;
        public double BATERIA_INICIO;
        public double BATERIA_FIN;
        public String ZONA_INI;
        public String ZONA_FIN;
        public double DISTANCIA;
        public double KMINI;
        public double KMFIN;
    }

    public static class FinalizarConduccionResponse extends GenericResponse<FinalizarConduccionData> {}

    public static class WalletMovement {
        public int ID;
        public String TIPO; // GASTO, REGALORECIBIR, etc.
        public String NUMERORESERVA;
        public double IMPORTE;
        public String MAP_IMPORTEVER;
        public String MAP_IMPORTEGRID;
        public String MAP_COLOR;
        public String MAP_FECHAINICIAL;
        public String MAP_FECHAFINAL;
        public String MAP_FECHA;
        public String MAP_HORA;
        public String MAP_FECHAHORA;
        public String MAP_TIPO;
        public String MAP_COCHE;
        public String MAP_KM;
        public double DISTANCIA;
        public String MAP_ESTADO;
        public String MAP_MODELO;
        public String MAP_COLORDINERO;
        public int MAP_YEAR;
        public int MAP_MES;
        public String MAP_BOOKINGDRIVINGTIME;
        public String MAP_BOOKINGSTANDBYTIME;
    }

    public static class WalletResponse extends GenericResponse<WalletMovement[]> {}

    public static class MinutePack {
        public int ID;
        public String PAQUETE;
        public double IMPORTE;
        public String CODIGO;
        public String MAP_TOTALTL; // Price to pay
        public String MAP_TOTALTL2; // Credit to receive
        public double MAP_TOTAL;
    }

    public static class MinutePacksResponse extends GenericResponse<MinutePack[]> {}

    public static class TracePoint {
        public double LATITUD;
        public double LONGITUD;
        public String I; // Icon
    }

    public static class TraceResponse extends GenericResponse<TracePoint[]> {}

    public static class ComprarPaqueteRequest {
        public String usuario;
        public String paquete;
        public String codigopromocion;
        public String codigoordenapp;

        public ComprarPaqueteRequest(String usuario, String paquete, String codigopromocion) {
            this.usuario = usuario;
            this.paquete = paquete;
            this.codigopromocion = codigopromocion;
            this.codigoordenapp = "";
        }
    }

    public static class HelpItem {
        public int ID;
        public String TITULO;
        public String DESCRIPCION;
        public String DOCUMENTO; // URL
        public String TIPO; // YOUTUBE, WEB
        public String MAP_IMG;
    }

    public static class HelpResponse extends GenericResponse<HelpItem[]> {}

    public static class RegalarSaldoRequest {
        public String userFrom;
        public String userTo;
        public double importe;

        public RegalarSaldoRequest(String userFrom, String userTo, double importe) {
            this.userFrom = userFrom;
            this.userTo = userTo;
            this.importe = importe;
        }
    }

    public static class DatosPersonalesRequest {
        public String USUARIO;
        public String NOMBRE;
        public String FECHANAC;
        public String DIRECCION;
        public String CP;
        public String POBLACION;
        public int IDSEDE;

        public DatosPersonalesRequest(String USUARIO, String NOMBRE, String FECHANAC, String DIRECCION, String CP, String POBLACION, int IDSEDE) {
            this.USUARIO = USUARIO;
            this.NOMBRE = NOMBRE;
            this.FECHANAC = FECHANAC;
            this.DIRECCION = DIRECCION;
            this.CP = CP;
            this.POBLACION = POBLACION;
            this.IDSEDE = IDSEDE;
        }
    }

    public static class SendSmsRequest {
        public String USUARIO;
        public String PREFIJO;
        public String TELEFONO;
        public String HASHAPK;

        public SendSmsRequest(String user, String pref, String phone) {
            this.USUARIO = user;
            this.PREFIJO = pref;
            this.TELEFONO = phone;
            this.HASHAPK = "hcVOukaex41";
        }
    }

    public static class UpdatePhoneRequest {
        public String USUARIO;
        public String PREFIJO;
        public String TELEFONO;
        public int SENDSMS; // Usually 0 when verifying code? Or 1? Request says 0.

        public UpdatePhoneRequest(String user, String pref, String phone) {
            this.USUARIO = user;
            this.PREFIJO = pref;
            this.TELEFONO = phone;
            this.SENDSMS = 0;
        }
    }
}
