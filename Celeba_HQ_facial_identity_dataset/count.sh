#!/bin/bash

echo ""
echo "Número de personas en train: $(ls ./train | wc -l)"
echo "Número de personas en test: $(ls ./test | wc -l)"
echo ""

# Función para calcular min, max y media de imágenes por persona
analyze_directory() {
    local dir=$1
    local label=$2
    
    echo "=== Análisis de $label ==="
    
    local counts=()
    for subdir in "$dir"/*; do
        if [ -d "$subdir" ]; then
            count=$(ls -1 "$subdir" 2>/dev/null | wc -l)
            counts+=($count)
        fi
    done
    
    if [ ${#counts[@]} -gt 0 ]; then
        local min=${counts[0]}
        local max=${counts[0]}
        local sum=0
        
        for count in "${counts[@]}"; do
            sum=$((sum + count))
            [ $count -lt $min ] && min=$count
            [ $count -gt $max ] && max=$count
        done
        
        local avg=$((sum / ${#counts[@]}))
        
        echo "Mínimo de imágenes por persona: $min"
        echo "Máximo de imágenes por persona: $max"
        echo "Media de imágenes por persona: $avg"
        echo "Total de imágenes: $sum"
        echo ""
    fi
}

analyze_directory "./train" "TRAIN"
analyze_directory "./test" "TEST"
