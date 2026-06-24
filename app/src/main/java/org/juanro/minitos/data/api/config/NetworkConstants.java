package org.juanro.minitos.data.api.config;

import org.juanro.minitos.R;

/**
 * Centralized network constants and API paths.
 */
public class NetworkConstants {
    
    // Base URLs
    public static final String MINITS_BASE_URL = "https://minits.es/minitscore";
    public static final String ORS_BASE_URL = "https://api.openrouteservice.org";

    // Minits API Paths
    public static final String PATH_AUTHENTICATE = "/api/redsys/AuthenticateApp";
    public static final String PATH_LOGIN = "/api/go/Login";
    public static final String PATH_ESTADO_CUENTA = "/api/app/EstadoCuenta";
    public static final String PATH_DATOS_SQL = "/api/app/DatosSQL";
    public static final String PATH_ZONAS = "/api/app/Zonas";
    public static final String PATH_RESERVAR = "/api/app/Reservar";
    public static final String PATH_CONDUCIR = "/api/app/Conducir";
    public static final String PATH_FINALIZAR = "/api/app/FinalizarConduccion";
    public static final String PATH_AYUDA = "/api/app/Ayuda";
    public static final String PATH_UPLOAD_FOTO = "/api/Upload/Foto";
    public static final String PATH_COMPRAR_PAQUETE = "/api/app/ComprarPaquete";
    public static final String PATH_REGALAR_SALDO = "/api/App/RegalarSaldo";
    public static final String PATH_DATOS_PERSONALES = "/api/App/DatosPersonales";
    public static final String PATH_SEND_CODE_SMS = "/api/App/SendCodigoSMS";
    public static final String PATH_UPDATE_PHONE = "/api/App/UpdatePhoneNew";

    // OpenRouteService Paths
    public static final String PATH_ORS_DIRECTIONS = "/v2/directions/foot-walking/geojson";

    // External URLs
    public static final String URL_FAQ = "https://minits.com/wp-content/preguntasfrecuentes.html";
    public static final String URL_FUNDACION_ATABAL = "https://www.fundacionatabal.org/";

    // Map Styles (Resource IDs)
    public static final int STYLE_LIBERTY = R.string.map_style_liberty;
    public static final int STYLE_DARK = R.string.map_style_dark;
    public static final int STYLE_BRIGHT = R.string.map_style_bright;
    public static final int STYLE_POSITRON = R.string.map_style_positron;
}
