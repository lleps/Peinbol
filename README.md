# Estructura (carpeta kotlin)

* **Box.kt** Clase de una Box, que tiene una x,y,z(position), sx,sy,sz(size), vx, vy, vz (velocidad)
    
* **Window.kt** Tiene lo relacionado a la ventana, los controles, y los graficos. Le podes agregar boxes para que dibuje,
cambiarle la posición de la cámara, etc.
                 
* **Physics.kt** Simula las fisicas (no esta hecho). Tiene una lista de boxes para dibujar. En su metodo simulate(delta)
procesa todas las boxes que tengan fisicas, y les suma x,y,z basado en su velocidad, les aplica gravedad,
hequea si colisionan con otras cajas para invertir sus velocidades, etc.
                 
* **Client.kt** Es la clase con el main loop. Crea una instancia de Window y Physics, y un par de cajas random en el mapa
