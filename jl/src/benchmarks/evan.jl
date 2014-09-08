using Sigma

X = uniform(0,0,10)
Y = uniform(1,0,10)

T1 = uniform(2,0, 20)
T2 = uniform(3,0, 20)

time1 = 7/4 * X
time2 = @If((X < T1),time1,
            @If((T1 < X) & (X < T2), X + Y,
                T2 + 7/4 * Y))


plot_cond_density(T2,time1 < time2, 0.,20.,n_bars = 40)

cond_prob_deep(T1>10, time2 < time1,n_bars =2)