.class Lcom/hnl/kamiverify/KamiVerifyActivity$1;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface$OnClickListener;
.source "KamiVerifyActivity.java"

.field final synthetic this$0:Lcom/hnl/kamiverify/KamiVerifyActivity;

.method constructor <init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    .registers 2
    iput-object p1, p0, Lcom/hnl/kamiverify/KamiVerifyActivity$1;->this$0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public onClick(Landroid/content/DialogInterface;I)V
    .registers 4
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity$1;->this$0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    iget-object v1, v0, Lcom/hnl/kamiverify/KamiVerifyActivity;->et:Landroid/widget/EditText;
    invoke-virtual {v1}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Lcom/hnl/kamiverify/KamiVerifyActivity;->check(Ljava/lang/String;)Z
    move-result v1
    if-eqz v1, :fail
    invoke-virtual {v0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->launch()V
    goto :end
    :fail
    const-string v1, "卡密错误，请重新输入"
    const/4 v2, 0x0
    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :end
    return-void
.end method
