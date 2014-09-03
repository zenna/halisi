using Sigma

breast_cancer = flip(1,0.01)
positive_mammogram = @If breast_cancer flip(2, 0.8) flip(3,0.096)
cond_prob_deep(breast_cancer, positive_mammogram, max_depth = 9)
