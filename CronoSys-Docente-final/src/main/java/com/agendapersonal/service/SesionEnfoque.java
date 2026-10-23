package com.agendapersonal.service;

/**
 * Máquina de estados de una sesión de enfoque (sin UI): fases, pausa, reinicio y avance de ciclo.
 *
 * <p>Cuando el método tiene rangos (por ejemplo "60–90 min"), al construir la sesión se sortea un valor
 * concreto dentro de cada rango y ese es el que se usa durante toda la sesión. También se puede fijar
 * a mano un valor exacto (para hacer una sola sesión más corta o más larga).</p>
 */
public final class SesionEnfoque {
    public enum Fase { TRABAJO, DESCANSO, DESCANSO_LARGO }

    private final MetodoEnfoque metodo;
    private final int trabajoUsado, descansoUsado, largoUsado;
    private Fase fase = Fase.TRABAJO;
    private int total;            // segundos de la fase (0 en flowtime mientras se trabaja)
    private int transcurrido;     // segundos
    private int sesion = 1;       // sesión de trabajo dentro del ciclo (1..n)
    private int ciclo = 1;
    private boolean pausada = true;
    private boolean terminada;

    /** Crea la sesión con los valores del método: si hay rango, se sortea uno dentro de él. */
    public SesionEnfoque(MetodoEnfoque metodo) {
        this(metodo, null, null, null);
    }

    /**
     * Crea la sesión fijando valores exactos (en minutos). Los que se dejen en null se toman del
     * método: el mínimo si no hay rango, o un valor sorteado si lo hay.
     */
    public SesionEnfoque(MetodoEnfoque metodo, Integer trabajo, Integer descanso, Integer descansoLargo) {
        this.metodo = metodo;
        this.trabajoUsado = trabajo != null ? trabajo
            : (metodo.rangoTrabajo() ? MetodoEnfoque.elegir(metodo.trabajoMin(), metodo.trabajoMax()) : metodo.trabajoMin());
        this.descansoUsado = descanso != null ? descanso
            : (metodo.rangoDescanso() ? MetodoEnfoque.elegir(metodo.descansoMin(), metodo.descansoMax()) : metodo.descansoMin());
        this.largoUsado = descansoLargo != null ? descansoLargo
            : (metodo.rangoDescansoLargo() ? MetodoEnfoque.elegir(metodo.descansoLargoMin(), metodo.descansoLargoMax()) : metodo.descansoLargoMin());
        iniciarFase(Fase.TRABAJO);
    }

    private void iniciarFase(Fase f) {
        fase = f;
        transcurrido = 0;
        total = switch (f) {
            case TRABAJO -> metodo.libre() ? 0 : trabajoUsado * 60;
            case DESCANSO -> descansoUsado * 60;
            case DESCANSO_LARGO -> largoUsado * 60;
        };
    }

    public MetodoEnfoque metodo() { return metodo; }
    public Fase fase() { return fase; }
    public int sesion() { return sesion; }
    public int ciclo() { return ciclo; }
    public boolean pausada() { return pausada; }
    public boolean terminada() { return terminada; }
    public int transcurrido() { return transcurrido; }
    public int total() { return total; }
    public boolean cuentaLibre() { return fase == Fase.TRABAJO && metodo.libre(); }

    /** Minutos de trabajo que se han sorteado (o fijado) para esta sesión. */
    public int trabajoUsado() { return trabajoUsado; }
    /** Minutos de descanso que se han sorteado (o fijado) para esta sesión. */
    public int descansoUsado() { return descansoUsado; }
    /** Minutos de descanso largo que se han sorteado (o fijado) para esta sesión. */
    public int largoUsado() { return largoUsado; }

    /** Segundos que faltan (en flowtime trabajando, los que lleva). */
    public int restantes() { return cuentaLibre() ? transcurrido : Math.max(0, total - transcurrido); }

    /** 0 a 1. En flowtime trabajando no hay meta, devuelve 0. */
    public double progreso() { return total <= 0 ? 0 : Math.min(1.0, transcurrido / (double) total); }

    public void iniciar() { if (!terminada) pausada = false; }
    public void pausar() { pausada = true; }

    /** Reinicia solo el tiempo de la fase actual. */
    public void reiniciarFase() { transcurrido = 0; pausada = true; }

    public void detener() { pausada = true; terminada = true; }

    /** Avanza el reloj. Devuelve true si cambió de fase (para avisar con sonido o notificación). */
    public boolean tick(int segundos) {
        if (pausada || terminada || segundos <= 0) return false;
        transcurrido += segundos;
        if (cuentaLibre() || transcurrido < total) return false;
        siguiente();
        return true;
    }

    /** Flowtime: termina el trabajo y calcula el descanso (un quinto de lo trabajado, mínimo 5 min). */
    public void terminarFlow() {
        if (!cuentaLibre()) return;
        int minutos = Math.max(5, Math.round(transcurrido / 60f / 5f));
        fase = Fase.DESCANSO;
        transcurrido = 0;
        total = minutos * 60;
    }

    /** Salta a la fase siguiente. */
    public void siguiente() {
        if (fase == Fase.TRABAJO) {
            boolean largo = metodo.sesionesPorCiclo() > 0 && largoUsado > 0 && sesion >= metodo.sesionesPorCiclo();
            if (largo) iniciarFase(Fase.DESCANSO_LARGO);
            else if (metodo.descansoMin() > 0 || metodo.libre()) {
                iniciarFase(Fase.DESCANSO);
                if (metodo.libre()) total = Math.max(5, descansoUsado) * 60;
            } else { sesion++; iniciarFase(Fase.TRABAJO); }
        } else {
            if (fase == Fase.DESCANSO_LARGO) { ciclo++; sesion = 1; } else sesion++;
            iniciarFase(Fase.TRABAJO);
        }
    }

    public static String formato(int segundos) {
        int h = segundos / 3600, m = (segundos % 3600) / 60, s = segundos % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }
}
