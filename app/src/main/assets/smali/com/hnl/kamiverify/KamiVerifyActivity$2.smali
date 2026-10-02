.class Lcom/hnl/kamiverify/KamiVerifyActivity$2;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;
.source "KamiVerifyActivity.java"

.field final synthetic this$0:Lcom/hnl/kamiverify/KamiVerifyActivity;

.method constructor <init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    .registers 2
    iput-object p1, p0, Lcom/hnl/kamiverify/KamiVerifyActivity$2;->this$0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public onClick(Landroid/content/DialogInterface;I)V
    .registers 2
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity$2;->this$0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    invoke-virtual {v0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->finish()V
    return-void
.end method
