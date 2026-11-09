# Análisis de temperaturas por ciudad

Aplicación Java de escritorio que carga `src/datos/temperaturas_capitales_2015_2024_v2.csv` y permite:

- Graficar el promedio de temperatura de cada ciudad en un rango de fechas inclusivo.
- Consultar las temperaturas por ciudad en una fecha y destacar la ciudad más y menos calurosa.
- Consultar la evolución año a año de una ciudad.
- Comparar los promedios anuales de dos ciudades en el rango seleccionado.

Las transformaciones y agregaciones de colecciones usan Streams (`filter`, `map`, `distinct`, `groupingBy` y `averagingDouble`). Las gráficas de barras están dibujadas con Swing, tienen colores por ciudad, animación al actualizar y resaltado de datos al pasar el cursor; no se requieren bibliotecas externas.

## Requisitos

- JDK 17 o posterior.
- Ejecutar desde la carpeta raíz del proyecto para que la aplicación encuentre el CSV en `src/datos`. También se puede pasar la ruta del archivo como argumento.

## Compilar y ejecutar en Windows PowerShell

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName)
java -cp bin App
```

Para indicar otro archivo CSV:

```powershell
java -cp bin App "C:\ruta\temperaturas.csv"
```

El archivo debe tener el encabezado `Ciudad,Fecha,Temperatura`, fechas en formato `dd/MM/yyyy` y puede incluir nombres de ciudad con comas entre comillas, como `"BOGOTA, D.C"`.
