.class public Lcom/hnl/kamiverify/KamiVerifyActivity;
.super Landroid/app/Activity;
.source "KamiVerifyActivity.java"

.field private et:Landroid/widget/EditText;
.field private list:Ljava/util/ArrayList;
.field private target:Ljava/lang/String;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 5
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    new-instance v0, Ljava/util/ArrayList;
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
    iput-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->list:Ljava/util/ArrayList;

    const-string v0, "TARGET_ACTIVITY_PLACEHOLDER"
    iput-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->target:Ljava/lang/String;

    :try_start
    invoke-virtual {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->getAssets()Landroid/content/res/AssetManager;
    move-result-object v0
    const-string v1, "kami_list.txt"
    invoke-virtual {v0, v1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
    move-result-object v0
    new-instance v1, Ljava/io/BufferedReader;
    new-instance v2, Ljava/io/InputStreamReader;
    invoke-direct {v2, v0}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V
    invoke-direct {v1, v2}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :read_loop
    invoke-virtual {v1}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :read_done
    iget-object v2, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->list:Ljava/util/ArrayList;
    invoke-virtual {v2, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    goto :read_loop
    :read_done
    invoke-virtual {v1}, Ljava/io/BufferedReader;->close()V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch_all

    :catch_all
    invoke-direct {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->showDialog()V
    return-void
.end method

.method private showDialog()V
    .registers 5
    new-instance v0, Landroid/app/AlertDialog$Builder;
    invoke-direct {v0, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    const-string v1, "VERIFY_TITLE_PLACEHOLDER"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    const-string v1, "VERIFY_SUBTITLE_PLACEHOLDER"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    new-instance v1, Landroid/widget/EditText;
    invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V
    iput-object v1, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->et:Landroid/widget/EditText;
    const-string v2, "请输入卡密"
    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog$Builder;

    new-instance v1, Lcom/hnl/kamiverify/KamiVerifyActivity$1;
    invoke-direct {v1, p0}, Lcom/hnl/kamiverify/KamiVerifyActivity$1;-><init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    const-string v2, "验证"
    invoke-virtual {v0, v2, v1}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    new-instance v1, Lcom/hnl/kamiverify/KamiVerifyActivity$2;
    invoke-direct {v1, p0}, Lcom/hnl/kamiverify/KamiVerifyActivity$2;-><init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    const-string v2, "退出"
    invoke-virtual {v0, v2, v1}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setCancelable(Z)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog$Builder;->create()Landroid/app/AlertDialog;
    move-result-object v0
    invoke-virtual {v0}, Landroid/app/AlertDialog;->show()V
    return-void
.end method

.method private check(Ljava/lang/String;)Z
    .registers 3
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->list:Ljava/util/ArrayList;
    invoke-virtual {v0, p1}, Ljava/util/ArrayList;->contains(Ljava/lang/Object;)Z
    move-result v0
    return v0
.end method

.method private launch()V
    .registers 4
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->target:Ljava/lang/String;
    if-eqz v0, :done
    new-instance v1, Landroid/content/Intent;
    invoke-direct {v1}, Landroid/content/Intent;-><init>()V
    invoke-virtual {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->getPackageName()Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v1, v2, v0}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
    const/high16 v2, 0x1000
    invoke-virtual {v1, v2}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
    invoke-virtual {p0, v1}, Lcom/hnl/kamiverify/KamiVerifyActivity;->startActivity(Landroid/content/Intent;)V
    invoke-virtual {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->finish()V
    :done
    return-void
.end method
