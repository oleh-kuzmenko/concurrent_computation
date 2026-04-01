import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def plot_results(file_path):
    df = pd.read_csv(file_path)
    sizes = df['Size'].unique()
    
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 14))
    plt.subplots_adjust(hspace=0.3)

    # 1. Графік часу виконання
    for size in sizes:
        subset = df[df['Size'] == size]
        ax1.plot(subset['Threads'], subset['TimeMs'], marker='o', label=f'Матриця {size}x{size}')
    
    ax1.set_xlabel('Кількість потоків (log2 scale)')
    ax1.set_ylabel('Час виконання (мс)')
    ax1.set_title('Залежність часу виконання від кількості потоків')
    ax1.legend()
    ax1.grid(True, which="both", ls="-", alpha=0.5)
    ax1.set_xscale('log', base=2)
    ax1.set_xticks(df['Threads'].unique())
    ax1.get_xaxis().set_major_formatter(plt.ScalarFormatter())

    # 2. Графік прискорення
    for size in sizes:
        subset = df[df['Size'] == size].copy()
        # Розрахунок S = T1 / Tn
        t1 = subset[subset['Threads'] == 1]['TimeMs'].values[0]
        subset['Speedup'] = t1 / subset['TimeMs']
        
        ax2.plot(subset['Threads'], subset['Speedup'], marker='s', label=f'Прискорення S ({size}x{size})')

    ax2.set_xlabel('Кількість потоків (log2 scale)')
    ax2.set_ylabel('Прискорення (S)')
    ax2.set_title('Експериментальне прискорення від кількості потоків')
    ax2.legend()
    ax2.grid(True, which="both", ls="-", alpha=0.5)
    ax2.set_xscale('log', base=2)
    ax2.set_xticks(df['Threads'].unique())
    ax2.get_xaxis().set_major_formatter(plt.ScalarFormatter())

    plt.savefig('results/performance_graph.png')
    plt.show()

if __name__ == "__main__":
    import os
    if not os.path.exists('results'):
        os.makedirs('results')
        
    plot_results('results/res.csv')
    